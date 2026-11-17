import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { access, mkdtemp, readFile, rm } from 'node:fs/promises';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { basename, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const target = process.env.DEMO_BROWSER_TARGET === 'wasmJs' ? 'wasmJs' : 'js';
const resources = join(root, `examples-compose/src/${target}Main/resources`);
const bundle = join(root, `examples-compose/build/kotlin-webpack/${target}/developmentExecutable`);
const webComponentBundle = join(root, 'webcomponent/build/kotlin-webpack/js/developmentExecutable');
const chromePath = process.env.CHROME_BIN ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function until(read, predicate, label, timeout = 60_000) {
  const started = Date.now();
  while (Date.now() - started < timeout) {
    const value = await read();
    if (predicate(value)) return value;
    await sleep(100);
  }
  throw new Error(`Timed out waiting for ${label}`);
}

function serveDemo() {
  return createServer(async (request, response) => {
    const path = new URL(request.url, 'http://localhost').pathname;
    const name = basename(path);
    const file = path === '/' ? join(resources, 'index.html')
      : path === '/webcomponent/register.js'
        ? join(root, 'webcomponent/src/jsMain/resources/register.js')
        : path === '/webcomponent/embed.html'
          ? join(root, 'webcomponent/src/jsMain/resources/embed.html')
        : path.startsWith('/webcomponent/') &&
            (name === 'jsonforms-kotlin-webcomponent.js' || name.endsWith('.wasm') || name.endsWith('.js'))
          ? join(webComponentBundle, name)
        : path === `/${name}` && (name === 'examples-compose.js' || name.endsWith('.wasm') || name.endsWith('.js'))
          ? join(bundle, name)
          : path === '/kotlin-logo.svg' ? join(resources, name) : null;
    if (!file) { response.writeHead(404).end(); return; }
    try {
      const type = name.endsWith('.wasm') ? 'application/wasm'
        : name.endsWith('.js') ? 'application/javascript'
          : name.endsWith('.svg') ? 'image/svg+xml' : 'text/html; charset=utf-8';
      response.writeHead(200, { 'content-type': type });
      response.end(await readFile(file));
    } catch { response.writeHead(404).end(); }
  });
}

class DevTools {
  constructor(socket) {
    this.socket = socket;
    this.nextId = 0;
    this.pending = new Map();
    socket.onmessage = event => {
      const message = JSON.parse(event.data);
      const resolve = this.pending.get(message.id);
      if (resolve) { this.pending.delete(message.id); resolve(message); }
    };
  }

  async send(method, params = {}) {
    const id = ++this.nextId;
    const result = new Promise(resolve => this.pending.set(id, resolve));
    this.socket.send(JSON.stringify({ id, method, params }));
    const message = await result;
    if (message.error) throw new Error(`${method}: ${message.error.message}`);
    return message.result;
  }

  async evaluate(expression) {
    const result = await this.send('Runtime.evaluate', {
      expression, returnByValue: true, awaitPromise: true,
    });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
    return result.result.value;
  }

  async namedNode(name) {
    const tree = await this.send('Accessibility.getFullAXTree');
    return tree.nodes.find(node => node.name?.value === name && node.backendDOMNodeId);
  }

  async click(name) {
    const node = await until(() => this.namedNode(name), Boolean, `${name} action`);
    const { model } = await this.send('DOM.getBoxModel', { backendNodeId: node.backendDOMNodeId });
    const x = (model.border[0] + model.border[2]) / 2;
    const y = (model.border[1] + model.border[5]) / 2;
    await this.clickAt(x, y);
  }

  async clickAt(x, y) {
    for (const type of ['mousePressed', 'mouseReleased']) {
      await this.send('Input.dispatchMouseEvent', { type, x, y, button: 'left', clickCount: 1 });
    }
  }

  async namedNodeInFrame(name, frameUrlPart) {
    const frames = await this.send('Page.getFrameTree');
    const frameId = frames.frameTree.childFrames?.find(child =>
      child.frame.url.includes(frameUrlPart))?.frame.id;
    if (!frameId) return undefined;
    const tree = await this.send('Accessibility.getFullAXTree', { frameId });
    return tree.nodes.find(node => node.name?.value === name && node.backendDOMNodeId);
  }

  async clickInFrame(name, frameUrlPart) {
    const node = await until(() => this.namedNodeInFrame(name, frameUrlPart), Boolean,
      `${name} in frame`);
    const { model } = await this.send('DOM.getBoxModel', { backendNodeId: node.backendDOMNodeId });
    // Chrome reports iframe AX box coordinates in page space.
    await this.clickAt((model.border[0] + model.border[2]) / 2,
      (model.border[1] + model.border[5]) / 2);
  }

  async screenshotRegionHash(x, y, width, height) {
    // Compose paints the form on a canvas; Chrome can temporarily return a sparse
    // accessibility tree after history navigation. Compare rendered value pixels.
    const { data } = await this.send('Page.captureScreenshot', { format: 'png' });
    return this.evaluate(`(async () => {
      const image = new Image();
      image.src = 'data:image/png;base64,${data}';
      await image.decode();
      const canvas = document.createElement('canvas');
      canvas.width = ${width}; canvas.height = ${height};
      const context = canvas.getContext('2d');
      context.drawImage(image, ${x}, ${y}, ${width}, ${height}, 0, 0, ${width}, ${height});
      const pixels = context.getImageData(0, 0, ${width}, ${height}).data;
      let hash = 2166136261;
      for (const pixel of pixels) hash = Math.imul(hash ^ pixel, 16777619);
      return hash >>> 0;
    })()`);
  }
}

test('demo URL controls form-only view and browser history', { timeout: 120_000 }, async () => {
  await access(join(bundle, 'examples-compose.js'));
  await access(chromePath);
  const server = serveDemo();
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  const port = server.address().port;
  const profile = await mkdtemp(join(tmpdir(), 'jsonforms-demo-chrome-'));
  const chrome = spawn(chromePath, [
    '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
    '--disable-dev-shm-usage', '--remote-debugging-port=0', `--user-data-dir=${profile}`,
    `http://127.0.0.1:${port}/#spec-pre-touch-errors?form-only=true&active-tab=schema`,
  ], { stdio: 'ignore' });
  let devtools;
  try {
    const activePort = await until(async () => {
      try { return (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
      catch { return null; }
    }, Boolean, 'Chrome DevTools');
    const page = await until(async () => {
      const tabs = await (await fetch(`http://127.0.0.1:${activePort}/json`)).json();
      return tabs.find(tab => tab.type === 'page' && tab.url.includes(`127.0.0.1:${port}/`));
    }, Boolean, 'demo page');
    const socket = new WebSocket(page.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    devtools = new DevTools(socket);
    await until(() => devtools.namedNode('Show full UI'), Boolean, 'form-only view');
    assert.equal(await devtools.namedNode('Open settings'), undefined);
    await devtools.click('Show full UI');
    const exited = await until(() => devtools.evaluate('location.hash'),
      hash => hash === '#spec-pre-touch-errors?active-tab=schema', 'canonical URL after exit');
    assert.equal(exited, '#spec-pre-touch-errors?active-tab=schema');
    assert.equal(await devtools.evaluate('location.search'), '');
    await until(() => devtools.namedNode('Open settings'), Boolean, 'full demo toolbar');
    await devtools.evaluate('history.back()');
    await until(() => devtools.namedNode('Show full UI'), Boolean, 'form-only view after Back');
    assert.equal(await devtools.namedNode('Open settings'), undefined);
    await devtools.evaluate('history.forward()');
    await until(() => devtools.namedNode('Open settings'), Boolean, 'full demo after Forward');
    assert.equal(await devtools.evaluate('location.hash'), '#spec-pre-touch-errors?active-tab=schema');
  } finally {
    devtools?.socket.close();
    chrome.kill('SIGTERM');
    if (chrome.exitCode === null && chrome.signalCode === null) {
      await new Promise(resolve => chrome.once('exit', resolve));
    }
    await new Promise(resolve => server.close(resolve));
    await rm(profile, { recursive: true, force: true, maxRetries: 10, retryDelay: 100 });
  }
});

test('Compose working data survives browser Back and Forward', { timeout: 120_000 }, async () => {
  await access(join(bundle, 'examples-compose.js'));
  await access(chromePath);
  const server = serveDemo();
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  const port = server.address().port;
  const profile = await mkdtemp(join(tmpdir(), 'jsonforms-demo-compose-history-'));
  const chrome = spawn(chromePath, [
    '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
    '--disable-dev-shm-usage', '--window-size=800,600', '--remote-debugging-port=0', `--user-data-dir=${profile}`,
    `http://127.0.0.1:${port}/#dynamic`,
  ], { stdio: 'ignore' });
  let devtools;
  try {
    const activePort = await until(async () => {
      try { return (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
      catch { return null; }
    }, Boolean, 'Chrome DevTools');
    const page = await until(async () => {
      const tabs = await (await fetch(`http://127.0.0.1:${activePort}/json`)).json();
      return tabs.find(tab => tab.type === 'page' && tab.url.includes(`127.0.0.1:${port}/`));
    }, Boolean, 'demo page');
    const socket = new WebSocket(page.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    devtools = new DevTools(socket);
    // The same field moves upward by 252 screen pixels in form-only mode.
    const fullValue = () => devtools.screenshotRegionHash(40, 366, 500, 32);
    const formOnlyValue = () => devtools.screenshotRegionHash(40, 114, 500, 32);
    const originalValue = await fullValue();
    await devtools.click('Change data');
    await until(() => devtools.namedNode('aaa'), Boolean, 'changed Compose form data');
    const changedValue = await until(fullValue,
      value => value !== originalValue, 'rendered changed form value');
    await devtools.click('Show form only');
    await until(() => devtools.evaluate('location.hash'),
      hash => hash === '#dynamic?form-only=true', 'form-only history entry');
    await until(formOnlyValue, value => value === changedValue,
      'changed data in form-only view', 10_000);
    await devtools.evaluate('history.back()');
    await until(() => devtools.evaluate('location.hash'),
      hash => hash === '#dynamic', 'full-view history entry after Back');
    await until(fullValue, value => value === changedValue,
      'changed data after Back', 10_000);
    await devtools.evaluate('history.forward()');
    await until(() => devtools.evaluate('location.hash'),
      hash => hash === '#dynamic?form-only=true', 'form-only entry after Forward');
    await until(formOnlyValue, value => value === changedValue,
      'changed data after Forward', 10_000);
  } finally {
    devtools?.socket.close();
    chrome.kill('SIGTERM');
    if (chrome.exitCode === null && chrome.signalCode === null) {
      await new Promise(resolve => chrome.once('exit', resolve));
    }
    await new Promise(resolve => server.close(resolve));
    await rm(profile, { recursive: true, force: true, maxRetries: 10, retryDelay: 100 });
  }
});

test('form-only Web Component mode preserves its mounted form through history',
  { timeout: 120_000, skip: target !== 'js' }, async () => {
    await access(join(bundle, 'examples-compose.js'));
    await access(join(webComponentBundle, 'jsonforms-kotlin-webcomponent.js'));
    await access(chromePath);
    const server = serveDemo();
    await new Promise((resolve, reject) => {
      server.once('error', reject);
      server.listen(0, '127.0.0.1', resolve);
    });
    const port = server.address().port;
    const profile = await mkdtemp(join(tmpdir(), 'jsonforms-demo-webcomponent-'));
    const chrome = spawn(chromePath, [
      '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
      '--disable-dev-shm-usage', '--remote-debugging-port=0', `--user-data-dir=${profile}`,
      `http://127.0.0.1:${port}/#spec-pre-touch-errors?form-only=true&use-webcomponent=true`,
    ], { stdio: 'ignore' });
    let devtools;
    try {
      const activePort = await until(async () => {
        try { return (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
        catch { return null; }
      }, Boolean, 'Chrome DevTools');
      const page = await until(async () => {
        const tabs = await (await fetch(`http://127.0.0.1:${activePort}/json`)).json();
        return tabs.find(tab => tab.type === 'page' && tab.url.includes(`127.0.0.1:${port}/`));
      }, Boolean, 'demo page');
      const socket = new WebSocket(page.webSocketDebuggerUrl);
      await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
      devtools = new DevTools(socket);
      const overlay = `[data-jsonforms-demo-overlay]`;
      const bar = `[data-jsonforms-demo-bar]`;
      const exit = `[data-jsonforms-demo-exit]`;
      await until(() => devtools.evaluate(`Boolean(document.querySelector('${overlay} iframe')
        ?.contentWindow?.customElements?.get('jsonforms-kotlin-compose'))`), Boolean, 'mounted Web Component');
      await until(() => devtools.evaluate(`getComputedStyle(document.querySelector('${bar}')).display`),
        value => value === 'none', 'hidden demo bar');
      assert.equal(await devtools.evaluate(`getComputedStyle(document.querySelector('${exit}')).display`), 'block');
      await devtools.evaluate(`window.__demoForm = document.querySelector('${overlay} iframe')
        .contentDocument.querySelector('jsonforms-kotlin-compose')`);
      assert.equal(await devtools.evaluate('window.__demoForm.getAttribute("validation-mode")'),
        'validate-and-show');
      assert.equal(await devtools.evaluate('window.__demoForm.hasAttribute("data-provided")'), false);
      assert.equal(await devtools.evaluate(`JSON.parse(window.__demoForm.getAttribute('config')).restrict`), true);
      await until(() => devtools.namedNode('Required fields'), Boolean,
        'rendered Web Component form');
      await devtools.evaluate(`window.__demoErrors=[];
        window.addEventListener('error', event => window.__demoErrors.push(event.message));
        window.addEventListener('unhandledrejection', event => window.__demoErrors.push(String(event.reason)));`);
      await devtools.evaluate(`window.__demoForm.dispatchEvent(new
        window.__demoForm.ownerDocument.defaultView.CustomEvent('jsonforms-change',
          {detail:{data:{name:'Updated through iframe'}}}))`);
      const originalData = await until(() => devtools.evaluate('window.__demoForm.getAttribute("data")'),
        value => value?.includes('Updated through iframe'), 'bridged Web Component data change');
      await devtools.evaluate(`document.querySelector('${exit}').click()`);
      await until(() => devtools.evaluate('location.hash'),
        hash => hash === '#spec-pre-touch-errors?use-webcomponent=true', 'full Web Component URL');
      try {
        await until(() => devtools.evaluate(`getComputedStyle(document.querySelector('${bar}')).display`),
          value => value === 'flex', 'full Web Component bar', 5_000);
      } catch (error) {
        const state = await devtools.evaluate(`({hash:location.hash,errors:window.__demoErrors,
          overlays:Array.from(
          document.querySelectorAll('${overlay}')).map(root => ({
            bar:getComputedStyle(root.querySelector('${bar}')).display,
            exit:getComputedStyle(root.querySelector('${exit}')).display,
            formOnly:root.dataset.formOnly,
          }))})`);
        state.toolbar = Boolean(await devtools.namedNode('Open settings'));
        state.formOnlyExit = Boolean(await devtools.namedNode('Show full UI'));
        throw new Error(`${error.message}: ${JSON.stringify(state)}`);
      }
      assert.equal(await devtools.evaluate(`window.__demoForm === document.querySelector('${overlay} iframe')
        .contentDocument.querySelector('jsonforms-kotlin-compose')`), true);

      await devtools.evaluate('history.back()');
      await until(() => devtools.evaluate(`getComputedStyle(document.querySelector('${bar}')).display`),
        value => value === 'none', 'form-only Web Component after Back');
      assert.equal(await devtools.evaluate('window.__demoForm.getAttribute("data")'), originalData);
      await devtools.evaluate('history.forward()');
      await until(() => devtools.evaluate(`getComputedStyle(document.querySelector('${bar}')).display`),
        value => value === 'flex', 'full Web Component after Forward');
      await devtools.evaluate(`location.hash =
        '#spec-pre-touch-errors?use-webcomponent=true&read-only=true&active-tab=data&drawer=true'`);
      await until(() => devtools.evaluate('window.__demoForm.hasAttribute("readonly")'), Boolean,
        'Web Component read-only URL state');
      assert.equal(await devtools.evaluate('window.__demoForm.getAttribute("data")'), originalData);
      await devtools.evaluate('history.back()');
      await until(() => devtools.evaluate('window.__demoForm.hasAttribute("readonly")'),
        value => value === false, 'Web Component editable after Back');
      assert.equal(await devtools.evaluate('window.__demoForm.getAttribute("data")'), originalData);
      await devtools.evaluate(`document.querySelector('${bar} button').click()`);
      await until(() => devtools.evaluate(`document.querySelector('${overlay}') === null`),
        Boolean, 'returned Compose form');
      await devtools.evaluate(`location.hash = '#spec-pre-touch-errors?active-tab=data'`);
      await until(() => devtools.namedNode('Reload Data'), Boolean, 'Compose Data editor');
      await until(async () => (await devtools.send('Accessibility.getFullAXTree')).nodes
        .some(node => String(node.value?.value ?? node.name?.value ?? '')
          .includes('Updated through iframe')),
        Boolean, 'Web Component edit in Compose Data editor');
      await devtools.evaluate(`location.hash = '#spec-pre-touch-errors?use-webcomponent=true&active-tab=data'`);
      await until(() => devtools.evaluate(`document.querySelector('${overlay} iframe')
        ?.contentDocument?.querySelector('jsonforms-kotlin-compose')?.getAttribute('data')`),
        value => value === originalData, 'Compose data after reopening Web Component');
      await devtools.evaluate(`(() => {
        const form = document.querySelector('${overlay} iframe').contentDocument
          .querySelector('jsonforms-kotlin-compose');
        form.dispatchEvent(new form.ownerDocument.defaultView.CustomEvent('jsonforms-change',
          { detail: { data: null, dataProvided: false } }));
      })()`);
      await until(() => devtools.evaluate(`document.querySelector('${overlay} iframe')
        ?.contentDocument?.querySelector('jsonforms-kotlin-compose')?.getAttribute('data-provided')`),
        value => value === 'false', 'absent data in Web Component');
      assert.equal(await devtools.evaluate(`document.querySelector('${overlay} iframe')
        .contentDocument.querySelector('jsonforms-kotlin-compose').getAttribute('data')`), 'null');
      await devtools.evaluate(`document.querySelector('${bar} button').click()`);
      await until(() => devtools.evaluate(`document.querySelector('${overlay}') === null`),
        Boolean, 'absent data returned to Compose');
      await devtools.evaluate(`location.hash = '#spec-pre-touch-errors?active-tab=data'`);
      await until(() => devtools.namedNode('Reload Data'), Boolean, 'absent Compose Data editor');
      await devtools.evaluate(`location.hash = '#spec-pre-touch-errors?use-webcomponent=true&active-tab=data'`);
      await until(() => devtools.evaluate(`document.querySelector('${overlay} iframe')
        ?.contentDocument?.querySelector('jsonforms-kotlin-compose')?.getAttribute('data-provided')`),
        value => value === 'false', 'absent data after reopening Web Component');
      assert.equal(await devtools.evaluate(`document.querySelector('${overlay} iframe')
        .contentDocument.querySelector('jsonforms-kotlin-compose').getAttribute('data')`), 'null');
      const frames = await devtools.send('Page.getFrameTree');
      const frameId = frames.frameTree.childFrames?.find(child =>
        child.frame.url.includes('webcomponent/embed.html'))?.frame.id;
      assert.ok(frameId, 'Web Component iframe is present');
      const legalName = await until(async () => {
        const fieldTree = await devtools.send('Accessibility.getFullAXTree', { frameId });
        return fieldTree.nodes.find(node => node.role?.value === 'textbox' &&
          node.name?.value === 'Legal name' && node.backendDOMNodeId);
      }, Boolean, 'accessible Legal name input after absent data host switch', 10_000);
      const { model } = await devtools.send('DOM.getBoxModel',
        { backendNodeId: legalName.backendDOMNodeId });
      await devtools.clickAt((model.border[0] + model.border[2]) / 2,
        (model.border[1] + model.border[5]) / 2);
      await devtools.send('Input.insertText', { text: 'Grace' });
      await until(() => devtools.evaluate(`document.querySelector('${overlay} iframe')
        ?.contentDocument?.querySelector('jsonforms-kotlin-compose')?.getAttribute('data')`),
        value => value && JSON.parse(value)?.legalName === 'Grace',
        'nested edit from absent data in demo Web Component');
      assert.equal(await devtools.evaluate(`document.querySelector('${overlay} iframe')
        .contentDocument.querySelector('jsonforms-kotlin-compose').hasAttribute('data-provided')`), false);
      await devtools.evaluate(`document.querySelector('${bar} button').click()`);
      await until(() => devtools.evaluate(`document.querySelector('${overlay}') === null`),
        Boolean, 'edited formerly absent data returned to Compose');
      await devtools.evaluate(`location.hash = '#spec-pre-touch-errors?use-webcomponent=true&active-tab=data'`);
      await until(() => devtools.evaluate(`document.querySelector('${overlay} iframe')
        ?.contentDocument?.querySelector('jsonforms-kotlin-compose')?.getAttribute('data')`),
        value => value && JSON.parse(value)?.legalName === 'Grace',
        'provided edit after reopening Web Component');
      assert.equal(await devtools.evaluate(`document.querySelector('${overlay} iframe')
        .contentDocument.querySelector('jsonforms-kotlin-compose').hasAttribute('data-provided')`), false);
    } finally {
      devtools?.socket.close();
      chrome.kill('SIGTERM');
      if (chrome.exitCode === null && chrome.signalCode === null) {
        await new Promise(resolve => chrome.once('exit', resolve));
      }
      await new Promise(resolve => server.close(resolve));
      await rm(profile, { recursive: true, force: true, maxRetries: 10, retryDelay: 100 });
    }
  });

test('Job Web Component actions preserve form locale, theme and server errors',
  { timeout: 120_000, skip: target !== 'js' }, async () => {
    await access(join(bundle, 'examples-compose.js'));
    await access(join(webComponentBundle, 'jsonforms-kotlin-webcomponent.js'));
    const server = serveDemo();
    await new Promise((resolve, reject) => {
      server.once('error', reject);
      server.listen(0, '127.0.0.1', resolve);
    });
    const port = server.address().port;
    const profile = await mkdtemp(join(tmpdir(), 'jsonforms-demo-job-webcomponent-'));
    const chrome = spawn(chromePath, [
      '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
      '--disable-dev-shm-usage', '--remote-debugging-port=0', `--user-data-dir=${profile}`,
      `http://127.0.0.1:${port}/#spec-job-application?use-webcomponent=true`,
    ], { stdio: 'ignore' });
    let devtools;
    try {
      const activePort = await until(async () => {
        try { return (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
        catch { return null; }
      }, Boolean, 'Chrome DevTools');
      const page = await until(async () => {
        const tabs = await (await fetch(`http://127.0.0.1:${activePort}/json`)).json();
        return tabs.find(tab => tab.type === 'page' && tab.url.includes(`127.0.0.1:${port}/`));
      }, Boolean, 'Job Web Component page');
      const socket = new WebSocket(page.webSocketDebuggerUrl);
      await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
      devtools = new DevTools(socket);
      const form = `document.querySelector('[data-jsonforms-demo-overlay] iframe')
        ?.contentDocument?.querySelector('jsonforms-kotlin-compose')`;
      await until(() => devtools.evaluate(`Boolean(${form}?.getAttribute('uischemas'))`),
        Boolean, 'named Job UI schemas in Web Component');
      await devtools.evaluate(`window.__jobForm=${form}; window.__jobData=window.__jobForm.getAttribute('data');
        window.__jobBarBackground=document.querySelector('[data-jsonforms-demo-bar]').style.background;
        window.__jobActions=[]; window.__jobForm.addEventListener('jsonforms-action',
          event => window.__jobActions.push({action:event.detail.action,locale:event.detail.context.locale}));`);
      assert.ok((await devtools.evaluate(`JSON.parse(window.__jobForm.getAttribute('uischemas'))`))
        .some(entry => entry.uischema?.name));
      await until(() => devtools.evaluate(`document.querySelector(
        '[data-jsonforms-demo-bar] [role=status]')?.textContent`),
        value => value === 'Registered UI Schema tester functions are unavailable here',
        'registry tester limitation notice');
      assert.deepEqual(Object.keys(await devtools.evaluate(`JSON.parse(window.__jobForm.getAttribute('translations'))`)).sort(),
        ['bg', 'en']);
      assert.ok(await devtools.evaluate(`JSON.parse(window.__jobForm.getAttribute('config')) !== null`));
      await devtools.evaluate(`window.__jobForm.dispatchEvent(new
        window.__jobForm.ownerDocument.defaultView.CustomEvent('jsonforms-action',
          {detail:{action:'reportServer',params:{errors:[
            {instancePath:'/contact/email',message:'Injected host error'}]}}}))`);
      await until(() => devtools.evaluate(`JSON.parse(window.__jobForm.getAttribute('additional-errors'))`),
        errors => errors.some(error => error.message === 'Injected host error' &&
          error.params.owner === 'demo-server'), 'server error bridged to Web Component');
      await devtools.evaluate(`window.__jobForm.dispatchEvent(new
        window.__jobForm.ownerDocument.defaultView.CustomEvent('jsonforms-action',
          {detail:{action:'clearServer',params:{}}}))`);
      await until(() => devtools.evaluate(`JSON.parse(window.__jobForm.getAttribute('additional-errors'))`),
        errors => errors.every(error => error.params.owner !== 'demo-server'),
        'server error cleared in Web Component');
      await until(() => devtools.namedNodeInFrame('Български', 'webcomponent/embed.html'),
        Boolean, 'Job language button');
      await devtools.clickInFrame('Български', 'webcomponent/embed.html');
      await until(() => devtools.evaluate('window.__jobForm.getAttribute("lang")'),
        value => value === 'bg', 'Bulgarian form language');
      await until(() => devtools.namedNodeInFrame('Смени тема', 'webcomponent/embed.html'),
        Boolean, 'translated theme action', 10_000);
      const initialTheme = await devtools.evaluate('window.__jobForm.getAttribute("theme")');
      await devtools.clickInFrame('Смени тема', 'webcomponent/embed.html');
      await until(() => devtools.evaluate('window.__jobForm.getAttribute("theme")'),
        value => value === (initialTheme === 'dark' ? 'light' : 'dark'),
        'toggled form theme');
      const toggledTheme = await devtools.evaluate('window.__jobForm.getAttribute("theme")');
      await until(() => devtools.evaluate('window.__jobActions'),
        actions => actions.some(event => event.action === 'toggleMode' && event.locale === 'bg'),
        'renderer action context in Bulgarian');
      assert.equal(await devtools.evaluate('window.__jobForm.getAttribute("data")'),
        await devtools.evaluate('window.__jobData'));
      assert.equal(await devtools.evaluate('location.hash'),
        '#spec-job-application?use-webcomponent=true');
      assert.equal(await devtools.evaluate(`document.querySelector('[data-jsonforms-demo-bar]').style.background`),
        await devtools.evaluate('window.__jobBarBackground'));
      assert.equal(await devtools.evaluate(`document.querySelector('[data-jsonforms-demo-bar] span')?.textContent`),
        'Web component');
      await devtools.evaluate(`document.querySelector('[data-jsonforms-demo-bar] button').click()`);
      await until(() => devtools.evaluate(`document.querySelector('[data-jsonforms-demo-overlay]') === null`),
        Boolean, 'Job returned to Compose');
      await until(() => devtools.namedNode('Смени тема'), Boolean,
        'Bulgarian form action after returning to Compose');
      await devtools.evaluate(`location.hash = '#spec-job-application?use-webcomponent=true'`);
      await until(() => devtools.evaluate(`${form}?.getAttribute('lang')`),
        value => value === 'bg', 'form language after reopening Web Component');
      assert.equal(await devtools.evaluate(`${form}?.getAttribute('theme')`), toggledTheme);
      assert.equal(await devtools.evaluate(`${form}?.getAttribute('data')`),
        await devtools.evaluate('window.__jobData'));
    } finally {
      devtools?.socket.close();
      chrome.kill('SIGTERM');
      if (chrome.exitCode === null && chrome.signalCode === null) {
        await new Promise(resolve => chrome.once('exit', resolve));
      }
      await new Promise(resolve => server.close(resolve));
      await rm(profile, { recursive: true, force: true, maxRetries: 10, retryDelay: 100 });
    }
  });

for (const scenario of [
  {
    name: 'direct Data pane URL renders its editor with the drawer closed',
    route: '#spec-pre-touch-errors?active-tab=data&read-only=true&drawer=false',
    visible: 'Reload Data',
    hidden: 'Example source',
    menu: 'Show example menu',
  },
  {
    name: 'direct drawer URL opens the example menu',
    route: '#spec-pre-touch-errors?active-tab=data&drawer=true',
    visible: 'Example source',
    hidden: 'Reload Data',
    menu: 'Hide example menu',
  },
  {
    name: 'normal query URL selects the Data pane',
    route: '?active-tab=data&drawer=false#spec-pre-touch-errors',
    visible: 'Reload Data',
    hidden: 'Example source',
    menu: 'Show example menu',
  },
  {
    name: 'hash query overrides normal query for tab and drawer',
    route: '?active-tab=schema&drawer=true#spec-pre-touch-errors?active-tab=data&drawer=false',
    visible: 'Reload Data',
    hidden: 'Example source',
    menu: 'Show example menu',
  },
  {
    name: 'Settings read-only switch writes browser URL',
    route: '#spec-pre-touch-errors?active-tab=schema',
    visible: 'Reload Schema',
    hidden: 'Example source',
    menu: 'Show example menu',
    settings: true,
  },
  {
    name: 'tab and drawer actions write browser URL',
    route: '#spec-pre-touch-errors?active-tab=schema&drawer=false',
    visible: 'Reload Schema',
    hidden: 'Example source',
    menu: 'Show example menu',
    navigation: true,
  },
  {
    name: 'Settings overlay closes with Escape in Chrome',
    route: '#spec-pre-touch-errors?active-tab=schema',
    visible: 'Reload Schema',
    hidden: 'Example source',
    menu: 'Show example menu',
    settingsKeyboard: true,
  },
  {
    name: 'direction and validation settings survive Web Component host switching',
    route: '#spec-pre-touch-errors?active-tab=schema',
    visible: 'Reload Schema',
    hidden: 'Example source',
    menu: 'Show example menu',
    settingsBridge: true,
  },
  {
    name: 'Job form language action leaves demo chrome and URL unchanged',
    route: '#spec-job-application',
    visible: 'Български',
    hidden: 'Reload Data',
    menu: 'Show example menu',
    job: true,
  },
  {
    name: 'adapted spec fixture shows pinned source and capability notice',
    route: '#spec-additional-errors',
    visible: 'View source',
    hidden: 'Example source',
    menu: 'Show example menu',
    notice: 'Capability notice: Retains server-owned field errors and per-value clearing; excludes the Monaco editor diagnostic tabs.',
  },
]) test(scenario.name, { timeout: 120_000 }, async () => {
  const server = serveDemo();
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  const port = server.address().port;
  const profile = await mkdtemp(join(tmpdir(), 'jsonforms-demo-direct-data-'));
  const chrome = spawn(chromePath, [
    '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
    '--disable-dev-shm-usage', ...((scenario.settings || scenario.navigation ||
      scenario.settingsKeyboard || scenario.settingsBridge)
      ? ['--window-size=800,600'] : []),
    '--remote-debugging-port=0', `--user-data-dir=${profile}`,
    `http://127.0.0.1:${port}/${scenario.route}`,
  ], { stdio: 'ignore' });
  let devtools;
  try {
    const activePort = await until(async () => {
      try { return (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
      catch { return null; }
    }, Boolean, 'Chrome DevTools');
    const page = await until(async () => {
      const tabs = await (await fetch(`http://127.0.0.1:${activePort}/json`)).json();
      return tabs.find(tab => tab.type === 'page' && tab.url.includes(`127.0.0.1:${port}/`));
    }, Boolean, 'demo page');
    const socket = new WebSocket(page.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    devtools = new DevTools(socket);
    await until(() => devtools.namedNode(scenario.visible), Boolean, scenario.visible, 10_000);
    assert.equal(await devtools.namedNode(scenario.hidden), undefined);
    await until(() => devtools.namedNode(scenario.menu), Boolean, scenario.menu);
    if (scenario.settings) {
      await devtools.click('Open settings');
      await devtools.send('Input.dispatchMouseEvent',
        { type: 'mouseWheel', x: 450, y: 350, deltaY: 850, deltaX: 0 });
      await sleep(300);
      const readOnlyPixels = () => devtools.screenshotRegionHash(31, 367, 55, 38);
      const unchecked = await readOnlyPixels();
      await devtools.clickAt(56, 385);
      await devtools.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: 450, y: 135 });
      await until(() => devtools.evaluate('location.hash'),
        hash => hash === '#spec-pre-touch-errors?read-only=true&active-tab=schema',
        'read-only URL after Settings toggle');
      await sleep(400);
      const checked = await until(readOnlyPixels, value => value !== unchecked,
        'checked read-only switch', 10_000);
      await devtools.evaluate('history.back()');
      await until(() => devtools.evaluate('location.hash'),
        hash => hash === '#spec-pre-touch-errors?active-tab=schema',
        'editable URL after Back');
      await sleep(400);
      await until(readOnlyPixels, value => value === unchecked,
        'unchecked read-only switch after Back', 10_000);
      await devtools.evaluate('history.forward()');
      await until(() => devtools.evaluate('location.hash'),
        hash => hash === '#spec-pre-touch-errors?read-only=true&active-tab=schema',
        'read-only URL after Forward');
      await until(readOnlyPixels, value => value === checked,
        'checked read-only switch after Forward', 10_000);
    }
    if (scenario.settingsKeyboard) {
      await devtools.click('Open settings');
      await until(() => devtools.namedNode('Close settings'), Boolean, 'Settings close control');
      await sleep(300);
      await devtools.send('Input.dispatchKeyEvent', {
        type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27,
      });
      await devtools.send('Input.dispatchKeyEvent', {
        type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27,
      });
      await until(() => devtools.namedNode('Open settings'), Boolean, 'Settings reopened control');
      assert.equal(await devtools.namedNode('Close settings'), undefined);
      assert.equal(await devtools.evaluate('location.hash'),
        '#spec-pre-touch-errors?active-tab=schema');
    }
    if (scenario.settingsBridge) {
      await devtools.click('Open settings');
      await until(() => devtools.namedNode('Close settings'), Boolean, 'open Settings overlay');
      await devtools.clickAt(740, 264);
      await sleep(200);
      await devtools.clickAt(55, 375);
      await sleep(300);
      await devtools.send('Input.dispatchMouseEvent',
        { type: 'mouseWheel', x: 450, y: 350, deltaY: 430, deltaX: 0 });
      await sleep(300);
      await devtools.clickAt(700, 442);
      await devtools.send('Input.dispatchKeyEvent', {
        type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27,
      });
      await devtools.send('Input.dispatchKeyEvent', {
        type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27,
      });
      await until(() => devtools.namedNode('Close settings'),
        node => node === undefined, 'Settings closed after selecting validation mode');
      await devtools.evaluate(`location.hash = '#spec-pre-touch-errors?use-webcomponent=true&active-tab=schema'`);
      const form = `document.querySelector('[data-jsonforms-demo-overlay] iframe')
        ?.contentDocument?.querySelector('jsonforms-kotlin-compose')`;
      await until(() => devtools.evaluate(`${form}?.getAttribute('dir')`),
        value => value === 'rtl', 'RTL Web Component form');
      assert.equal(await devtools.evaluate(`${form}?.getAttribute('validation-mode')`),
        'validate-and-hide');
      await devtools.evaluate(`document.querySelector('[data-jsonforms-demo-bar] button').click()`);
      await until(() => devtools.evaluate(`document.querySelector('[data-jsonforms-demo-overlay]') === null`),
        Boolean, 'returned to Compose after Settings bridge');
      await devtools.evaluate(`location.hash = '#spec-pre-touch-errors?use-webcomponent=true&active-tab=schema'`);
      await until(() => devtools.evaluate(`${form}?.getAttribute('dir')`),
        value => value === 'rtl', 'RTL after reopening Web Component');
      assert.equal(await devtools.evaluate(`${form}?.getAttribute('validation-mode')`),
        'validate-and-hide');
    }
    if (scenario.navigation) {
      await devtools.click('Data');
      await until(() => devtools.evaluate('location.hash'),
        hash => hash === '#spec-pre-touch-errors?active-tab=data&drawer=false',
        'Data tab URL');
      await until(() => devtools.namedNode('Reload Data'), Boolean,
        'Data editor after tab click', 10_000);
      await sleep(300);
      const contentPixels = () => devtools.screenshotRegionHash(20, 90, 300, 90);
      const closedDrawer = await contentPixels();
      await devtools.clickAt(30, 28);
      await devtools.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: 450, y: 350 });
      await until(() => devtools.evaluate('location.hash'),
        hash => hash === '#spec-pre-touch-errors?active-tab=data&drawer=true',
        'open drawer URL');
      const openDrawer = await until(contentPixels, value => value !== closedDrawer,
        'rendered example menu', 10_000);
      await devtools.evaluate('history.back()');
      await until(() => devtools.evaluate('location.hash'),
        hash => hash === '#spec-pre-touch-errors?active-tab=data&drawer=false',
        'closed drawer URL after Back');
      await until(contentPixels, value => value === closedDrawer,
        'closed example menu after Back', 10_000);
      await devtools.evaluate('history.forward()');
      await until(() => devtools.evaluate('location.hash'),
        hash => hash === '#spec-pre-touch-errors?active-tab=data&drawer=true',
        'open drawer URL after Forward');
      await until(contentPixels, value => value === openDrawer,
        'open example menu after Forward', 10_000);
    }
    if (scenario.job) {
      await devtools.click('Български');
      await until(() => devtools.namedNode('Лична информация'), Boolean,
        'Bulgarian Job form category', 10_000);
      assert.equal(await devtools.evaluate('location.hash'), '#spec-job-application');
      await until(() => devtools.namedNode('Open settings'), Boolean,
        'English demo toolbar after form language action', 10_000);
    }
    if (scenario.notice) {
      await until(() => devtools.namedNode(scenario.notice), Boolean,
        'adaptation capability notice', 10_000);
      await devtools.evaluate('window.__openedSource=null; window.open=(url)=>{window.__openedSource=url; return null;}');
      await devtools.click('View source');
      const catalog = JSON.parse(await readFile(join(root, 'examples/spec/catalog.json'), 'utf8'));
      await until(() => devtools.evaluate('window.__openedSource'),
        value => value === `https://github.com/kchobantonov/jsonforms-extended-spec/tree/${catalog.commit}/examples/additional-errors`,
        'pinned source navigation', 10_000);
    }
    if (scenario.name.startsWith('direct Data pane')) {
      await devtools.click('7 validation errors. Show details');
      await until(async () => {
        const tree = await devtools.send('Accessibility.getFullAXTree');
        return tree.nodes.some(node => node.name?.value?.startsWith('Schema /'));
      }, Boolean, 'mounted validation details', 10_000);
    }
  } finally {
    devtools?.socket.close();
    chrome.kill('SIGTERM');
    if (chrome.exitCode === null && chrome.signalCode === null) {
      await new Promise(resolve => chrome.once('exit', resolve));
    }
    await new Promise(resolve => server.close(resolve));
    await rm(profile, { recursive: true, force: true, maxRetries: 10, retryDelay: 100 });
  }
});
