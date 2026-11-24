import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { access, readFile, mkdtemp, rm } from 'node:fs/promises';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const resources = join(root, 'webcomponent/src/jsMain/resources');
const bundleMode = process.env.WEBCOMPONENT_BUNDLE_MODE === 'production'
  ? 'productionExecutable' : 'developmentExecutable';
const bundle = join(root, 'webcomponent/build/kotlin-webpack/js', bundleMode);
const chromePath = process.env.CHROME_BIN ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function until(read, predicate, label, timeout = 30_000) {
  const started = Date.now();
  while (Date.now() - started < timeout) {
    const value = await read();
    if (predicate(value)) return value;
    await sleep(100);
  }
  throw new Error(`Timed out waiting for ${label}`);
}

function serveBundle() {
  const files = new Map([
    ['/', [join(resources, 'index.html'), 'text/html; charset=utf-8']],
    ['/register.js', [join(resources, 'register.js'), 'application/javascript']],
    ['/jsonforms-kotlin-webcomponent.js', [join(bundle, 'jsonforms-kotlin-webcomponent.js'), 'application/javascript']],
  ]);
  const server = createServer(async (request, response) => {
    const path = new URL(request.url, 'http://localhost').pathname;
    const entry = files.get(path) ?? (/^\/[a-f0-9]+\.(?:wasm|js)$/.test(path)
      ? [join(bundle, path.slice(1)), path.endsWith('.wasm')
        ? 'application/wasm' : 'application/javascript'] : null);
    if (!entry) { response.writeHead(404).end(); return; }
    try {
      response.writeHead(200, { 'content-type': entry[1] });
      const contents = await readFile(entry[0]);
      response.end(path === '/' ? contents.toString().replace(
        '<script src="register.js"></script>',
        `<script>
          document.querySelector('#contact-form').uiSchemaRegistry = [{
            tester: () => -1, uischema: { type: 'Label', text: 'Early registry' }
          }];
        </script><script src="register.js"></script>`,
      ) : contents);
    } catch { response.writeHead(404).end(); }
  });
  return server;
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

  async buttonBounds() {
    const tree = await this.send('Accessibility.getFullAXTree');
    const button = tree.nodes.find(node => node.role?.value === 'button' &&
      node.name?.value === 'Send action');
    if (!button?.backendDOMNodeId) return null;
    const { model } = await this.send('DOM.getBoxModel', { backendNodeId: button.backendDOMNodeId });
    return { x: (model.border[0] + model.border[2]) / 2,
      y: (model.border[1] + model.border[5]) / 2 };
  }

  async click(point) {
    const { x, y } = point;
    for (const type of ['mousePressed', 'mouseReleased']) {
      await this.send('Input.dispatchMouseEvent', { type, x, y, button: 'left', clickCount: 1 });
    }
  }

  async renderedButtonPixels(point) {
    const { data } = await this.send('Page.captureScreenshot', {
      format: 'png',
      clip: { x: Math.max(0, point.x - 60), y: Math.max(0, point.y - 24),
        width: 120, height: 48, scale: 1 },
    });
    return data;
  }
}

test('mounted web component preserves host actions and narrow duration popups', { timeout: 120_000 }, async () => {
  await access(join(bundle, 'jsonforms-kotlin-webcomponent.js'));
  await access(chromePath);
  const server = serveBundle();
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  const port = server.address().port;
  const profile = await mkdtemp(join(tmpdir(), 'jsonforms-wc-chrome-'));
  const chrome = spawn(chromePath, [
    '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
    '--disable-dev-shm-usage', '--remote-debugging-port=0', `--user-data-dir=${profile}`,
    `http://127.0.0.1:${port}/`,
  ], { stdio: 'ignore' });
  let devtools;
  try {
    const activePort = await until(async () => {
      try { return (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
      catch { return null; }
    }, Boolean, 'Chrome DevTools');
    const page = await until(async () => {
      const tabs = await (await fetch(`http://127.0.0.1:${activePort}/json`)).json();
      return tabs.find(tab => tab.type === 'page' && tab.url === `http://127.0.0.1:${port}/`);
    }, Boolean, 'test page');
    const socket = new WebSocket(page.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    devtools = new DevTools(socket);
    await until(() => devtools.evaluate("document.querySelector('#contact-form')?.getAttribute('uischema')"),
      Boolean, 'authored UI schema');
    const earlyRegistry = await until(() => devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      return form?._jsonformsMounted ? {
        ownProperty:Object.prototype.hasOwnProperty.call(form,'uiSchemaRegistry'),
        count:form.uiSchemaRegistry.length,
        testerRank:form.uiSchemaRegistry[0]?.tester?.(),
      } : null;
    })()`), Boolean, 'pre-upgrade registry after mount');
    assert.deepEqual(earlyRegistry, { ownProperty: false, count: 1, testerRank: -1 });
    const shadow = await until(() => devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      const root=form?.shadowRoot;
      const composeRoot=root?.firstElementChild?.firstElementChild?.firstElementChild?.shadowRoot;
      const canvas=composeRoot?.querySelector('canvas');
      return root && canvas ? {
        lightChildren:form.childElementCount,
        canvasDisplay:getComputedStyle(canvas).display,
        hasBackingInputStyle:composeRoot.querySelector('style')?.textContent
          .includes('.compose-backing-field'),
      } : null;
    })()`), Boolean, 'ShadowRoot Compose canvas');
    assert.deepEqual(shadow, {
      lightChildren: 0, canvasDisplay: 'block', hasBackingInputStyle: true,
    });
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      const ui=JSON.parse(form.getAttribute('uischema'));
      ui.elements[1].rule={effect:'SHOW',condition:{scope:'#/properties/name',expectedValue:'Ada'}};
      ui.elements[1]['x-action-meta']={source:'authored'};
      form.setAttribute('uischema',JSON.stringify(ui));
      const schema=JSON.parse(form.getAttribute('schema'));
      schema.properties.name.minLength=4;
      form.setAttribute('schema',JSON.stringify(schema));
      form.setAttribute('config',JSON.stringify({testing:{value:3}}));
      form.setAttribute('context',JSON.stringify({external:{tenant:'A'},custom:{flag:true}}));
      form.setAttribute('additional-errors',JSON.stringify([{
        instancePath:'/name',message:'Host rejected name',params:{owner:'server'}
      }]));
      form.setAttribute('lang','de');
    })()`);
    await sleep(300);
    const point = await until(() => devtools.buttonBounds(), Boolean, 'rendered Button');
    const pagePixels = () => devtools.renderedButtonPixels(point);
    await devtools.evaluate("document.querySelector('#contact-form').setAttribute('theme','light')");
    await sleep(200);
    const lightPagePixels = await pagePixels();
    await devtools.evaluate("document.querySelector('#contact-form').setAttribute('theme','dark')");
    await until(pagePixels, pixels => pixels !== lightPagePixels, 'dark theme form');
    const darkPagePixels = await pagePixels();
    await devtools.send('Emulation.setEmulatedMedia', {
      features: [{ name: 'prefers-color-scheme', value: 'light' }],
    });
    await devtools.evaluate("document.querySelector('#contact-form').setAttribute('theme','system')");
    await until(pagePixels, pixels => pixels === lightPagePixels, 'system light theme');
    await devtools.send('Emulation.setEmulatedMedia', {
      features: [{ name: 'prefers-color-scheme', value: 'dark' }],
    });
    await until(pagePixels, pixels => pixels === darkPagePixels, 'system dark theme after mount');
    await devtools.send('Emulation.setEmulatedMedia', {
      features: [{ name: 'prefers-color-scheme', value: 'light' }],
    });
    await until(pagePixels, pixels => pixels === lightPagePixels, 'system light theme after mount');
    await devtools.evaluate("document.querySelector('#contact-form').setAttribute('theme','light')");
    await devtools.evaluate(`window.__wcProbe={events:[],resolve:null,mode:'hold',releaseOther:null,errors:[]};
      window.addEventListener('error', event => window.__wcProbe.errors.push(event.message));
      window.addEventListener('unhandledrejection', event =>
        window.__wcProbe.errors.push(String(event.reason)));
      document.querySelector('#contact-form').addEventListener('jsonforms-action', event => {
        window.__wcProbe.events.push({action:event.detail.action,params:event.detail.params,
          label:event.detail.label,path:event.detail.path,element:event.detail.element,
          context:event.detail.context});
        if (window.__wcProbe.mode==='reject') {
          event.detail.waitUntil(Promise.reject(new Error('host rejected')));
          event.detail.waitUntil(new Promise(resolve => { window.__wcProbe.releaseOther=resolve; }));
        } else {
          event.detail.waitUntil(new Promise(resolve => { window.__wcProbe.resolve=resolve; }));
        }
      })`);

    await devtools.click(point);
    await until(() => devtools.evaluate('window.__wcProbe.events.length'), count => count === 1,
      'first action');
    const first = await devtools.evaluate('window.__wcProbe.events[0]');
    assert.equal(first.action, 'submitContact');
    assert.deepEqual(first.params, { source: 'webcomponent-demo' });
    assert.equal(first.label, 'Send action');
    assert.equal(first.path, '');
    assert.equal(first.element.type, 'Button');
    assert.equal(first.element.rule.effect, 'SHOW');
    assert.equal(first.element.rule.condition.expectedValue, 'Ada');
    assert.deepEqual(first.element['x-action-meta'], { source: 'authored' });
    assert.equal(first.context.data.name, 'Ada');
    assert.equal(first.context.schema.type, 'object');
    assert.equal(first.context.uischema.type, 'VerticalLayout');
    assert.equal(first.context.readonly, false);
    assert.equal(first.context.locale, 'en');
    assert.equal(first.context.external.tenant, 'A');
    assert.deepEqual(first.context.custom, { flag: true });
    assert.deepEqual(first.context.config, { testing: { value: 3 } });
    assert.equal(first.context.locale, 'en'); // Unsupported German resolves to English.
    assert.ok(Array.isArray(first.context.errors));
    assert.ok(first.context.errors.some(error => error.keyword === 'minLength' &&
      error.instancePath === '/name'));
    assert.ok(Array.isArray(first.context.additionalErrors));
    assert.deepEqual(first.context.additionalErrors, [{
      instancePath: '/name', message: 'Host rejected name', params: { owner: 'server' },
    }]);

    await devtools.click(point);
    await sleep(200);
    assert.equal(await devtools.evaluate('window.__wcProbe.events.length'), 1);
    await devtools.evaluate('window.__wcProbe.resolve()');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      form.setAttribute('lang','bg_BG');
      form.setAttribute('validation-mode','no-validation');
      form.setAttribute('context',JSON.stringify({external:{tenant:'B'},custom:{flag:false}}));
    })()`);
    await sleep(200);
    await devtools.click(point);
    await until(() => devtools.evaluate('window.__wcProbe.events.length'), count => count === 2,
      'action after completion');
    assert.equal(await devtools.evaluate('window.__wcProbe.events[1].context.locale'), 'bg');
    assert.deepEqual(await devtools.evaluate('window.__wcProbe.events[1].context.errors'), []);
    assert.equal(await devtools.evaluate('window.__wcProbe.events[1].context.external.tenant'), 'B');
    assert.equal(await devtools.evaluate('window.__wcProbe.events[0].context.external.tenant'), 'A');
    await devtools.evaluate('window.__wcProbe.resolve()');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      form.setAttribute('lang','en');
      form.setAttribute('validation-mode','validate-and-hide');
    })()`);
    await sleep(200);

    await devtools.evaluate("window.__wcProbe.mode='reject'");
    await devtools.click(point);
    await until(() => devtools.evaluate('window.__wcProbe.events.length'), count => count === 3,
      'rejected action');
    assert.ok((await devtools.evaluate('window.__wcProbe.events[2].context.errors'))
      .some(error => error.keyword === 'minLength' && error.instancePath === '/name'));
    await devtools.click(point);
    await sleep(200);
    assert.equal(await devtools.evaluate('window.__wcProbe.events.length'), 3);
    await devtools.evaluate('window.__wcProbe.releaseOther()');
    await until(() => devtools.evaluate('window.__wcProbe.errors'),
      errors => errors.some(message => message.includes('host rejected')),
      'propagated host rejection');
    await sleep(200);
    await devtools.evaluate("window.__wcProbe.mode='hold'");
    await devtools.click(point);
    try {
      await until(() => devtools.evaluate('window.__wcProbe.events.length'), count => count === 4,
        'action after rejection', 5_000);
    } catch (error) {
      const state = await devtools.evaluate('({count:window.__wcProbe.events.length,errors:window.__wcProbe.errors,html:document.querySelector("#contact-form").innerHTML.slice(0,500)})');
      throw new Error(`${error.message}: ${JSON.stringify(state)}`);
    }
    await devtools.evaluate('window.__wcProbe.resolve()');

    const scriptBody = `this.$el.dispatchEvent(new CustomEvent('jsonforms-script-start'));
      await new Promise(resolve => { window.__wcScript.release=resolve; });
      this.$el.dispatchEvent(new CustomEvent('jsonforms-script-result', {detail: {
        action:this.action, value:this.params.value, locale:this.context.locale,
        type:this.element.type, host:this.$el.tagName
      }}));`;
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      window.__wcScript={starts:0,results:[],diagnostics:[],release:null};
      form.addEventListener('jsonforms-script-start', () => window.__wcScript.starts++);
      form.addEventListener('jsonforms-script-result', event =>
        window.__wcScript.results.push(event.detail));
      form.addEventListener('jsonforms-diagnostic', event =>
        window.__wcScript.diagnostics.push(event.detail));
      const ui=JSON.parse(form.getAttribute('uischema'));
      ui.elements[1]={type:'Button',label:'Send action',script:${JSON.stringify(scriptBody)},
        params:{value:42}};
      form.setAttribute('uischema',JSON.stringify(ui));
    })()`);
    await until(() => devtools.evaluate('window.__wcScript.diagnostics'),
      diagnostics => diagnostics.some(diagnostic => diagnostic.code === 'button.evaluationDisabled'),
      'disabled script diagnostic');
    await devtools.click(point);
    await sleep(200);
    assert.equal(await devtools.evaluate('window.__wcScript.starts'), 0);

    await devtools.evaluate(`document.querySelector('#contact-form').setAttribute('config',
      JSON.stringify({jsonformsExtended:{security:{allowScriptEvaluation:'true'}}}))`);
    await sleep(200);
    await devtools.click(point);
    assert.equal(await devtools.evaluate('window.__wcScript.starts'), 0);

    await devtools.evaluate(`document.querySelector('#contact-form').setAttribute('config',
      JSON.stringify({jsonformsExtended:{security:{allowScriptEvaluation:true}}}))`);
    await sleep(200);
    await devtools.click(point);
    await until(() => devtools.evaluate('window.__wcScript.starts'), count => count === 1,
      'trusted script start');
    await devtools.click(point);
    await sleep(200);
    assert.equal(await devtools.evaluate('window.__wcScript.starts'), 1);
    await devtools.evaluate('window.__wcScript.release()');
    const results = await until(() => devtools.evaluate('window.__wcScript.results'),
      values => values.length === 1, 'trusted script completion');
    assert.deepEqual(results[0], { action: '', value: 42, locale: 'en',
      type: 'Button', host: 'JSONFORMS-KOTLIN-COMPOSE' });

    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      const ui=JSON.parse(form.getAttribute('uischema'));
      ui.elements[1].script='await Promise.resolve(); throw new Error("script rejected")';
      form.setAttribute('uischema',JSON.stringify(ui));
    })()`);
    await sleep(200);
    await devtools.click(point);
    await until(() => devtools.evaluate('window.__wcProbe.errors'),
      errors => errors.some(message => message.includes('script rejected')),
      'propagated script rejection');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      const ui=JSON.parse(form.getAttribute('uischema'));
      ui.elements[1].script=${JSON.stringify(scriptBody)};
      form.setAttribute('uischema',JSON.stringify(ui));
    })()`);
    await sleep(200);
    await devtools.click(point);
    await until(() => devtools.evaluate('window.__wcScript.starts'), count => count === 2,
      'script after rejection');
    await devtools.evaluate('window.__wcScript.release()');
    await sleep(200);

    const diagnosticCount = await devtools.evaluate('window.__wcScript.diagnostics.length');
    await devtools.evaluate(`(() => {
      const csp=document.createElement('meta');
      csp.httpEquiv='Content-Security-Policy';
      csp.content="script-src 'self'";
      document.head.appendChild(csp);
    })()`);
    await devtools.click(point);
    const cspDiagnostics = await until(() => devtools.evaluate('window.__wcScript.diagnostics'),
      diagnostics => diagnostics.length > diagnosticCount,
      'CSP script diagnostic');
    assert.equal(cspDiagnostics.at(-1).code, 'button.evaluationDisabled');
    assert.equal(await devtools.evaluate('window.__wcScript.starts'), 2);
    await sleep(200);
    await devtools.click(point);
    await until(() => devtools.evaluate('window.__wcScript.diagnostics.length'),
      count => count > cspDiagnostics.length, 'CSP rejection recovery');
    assert.equal(await devtools.evaluate('window.__wcScript.starts'), 2);

    const ltrButton = await devtools.buttonBounds();
    const direction = await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      form.setAttribute('dir','rtl');
      return {attribute:form.getAttribute('dir'),computed:getComputedStyle(form).direction};
    })()`);
    assert.deepEqual(direction, { attribute: 'rtl', computed: 'rtl' });
    const rtlButton = await until(() => devtools.buttonBounds(),
      point => point && point.x > ltrButton.x + 10, 'RTL button placement');
    assert.ok(rtlButton.x > ltrButton.x + 10);
    assert.equal(await devtools.evaluate(`JSON.parse(document.querySelector('#contact-form')
      .getAttribute('data')).name`), 'Ada');

    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      window.__presenceChanges=[];
      form.addEventListener('jsonforms-change', event =>
        window.__presenceChanges.push(event.detail));
      form.setAttribute('data','null');
      form.setAttribute('data-provided','false');
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/name'}));
    })()`);
    await sleep(300);
    const fieldTree = await devtools.send('Accessibility.getFullAXTree');
    const field = fieldTree.nodes.find(node => node.role?.value === 'textbox' &&
      node.backendDOMNodeId);
    assert.ok(field, `missing-data text field: ${JSON.stringify(fieldTree.nodes
      .map(node => [node.role?.value, node.name?.value]).slice(0, 40))}`);
    const { model: fieldModel } = await devtools.send('DOM.getBoxModel',
      { backendNodeId: field.backendDOMNodeId });
    await devtools.click({ x: (fieldModel.border[0] + fieldModel.border[2]) / 2,
      y: (fieldModel.border[1] + fieldModel.border[5]) / 2 });
    await devtools.send('Input.insertText', { text: 'Grace' });
    const changes = await until(() => devtools.evaluate('window.__presenceChanges'),
      values => values.some(change => change.data?.name === 'Grace'),
      'missing data becomes provided');
    assert.equal(changes.at(-1).dataProvided, true);
    assert.equal(changes.at(-1).data.name, 'Grace');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      window.__retainedShadow=form.shadowRoot;
      form.remove();
      document.querySelector('main section').appendChild(form);
    })()`);
    await until(() => devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      return form.shadowRoot===window.__retainedShadow &&
        form.shadowRoot.childElementCount===2;
    })()`), Boolean, 'single ShadowRoot viewport after reconnect');
    const reconnectedField = await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.find(node => node.role?.value === 'textbox' && node.backendDOMNodeId);
    }, Boolean, 'text input after reconnect', 5_000);
    const changesBeforeReconnectEdit = await devtools.evaluate('window.__presenceChanges.length');
    const { model: reconnectedFieldModel } = await devtools.send('DOM.getBoxModel',
      { backendNodeId: reconnectedField.backendDOMNodeId });
    await devtools.click({
      x: (reconnectedFieldModel.border[0] + reconnectedFieldModel.border[2]) / 2,
      y: (reconnectedFieldModel.border[1] + reconnectedFieldModel.border[5]) / 2,
    });
    await devtools.send('Input.insertText', { text: 'X' });
    const reconnectChanges = await until(() => devtools.evaluate('window.__presenceChanges'),
      values => values.length > changesBeforeReconnectEdit, 'edit after reconnect');
    assert.equal(reconnectChanges.length, changesBeforeReconnectEdit + 1);
    assert.equal(reconnectChanges.at(-1).dataProvided, true);
    assert.equal(reconnectChanges.at(-1).data.name.length, 'Grace'.length + 1);
    assert.ok(reconnectChanges.at(-1).data.name.includes('X'));
    await sleep(200);
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      form.remove();
      form.setAttribute('data','null');
      form.setAttribute('data-provided','false');
      document.querySelector('main section').appendChild(form);
    })()`);
    const resetField = await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.find(node => node.role?.value === 'textbox' && node.backendDOMNodeId);
    }, Boolean, 'host reset after detached attribute update', 5_000);
    await sleep(200);
    const { model: resetFieldModel } = await devtools.send('DOM.getBoxModel',
      { backendNodeId: resetField.backendDOMNodeId });
    await devtools.click({ x: (resetFieldModel.border[0] + resetFieldModel.border[2]) / 2,
      y: (resetFieldModel.border[1] + resetFieldModel.border[5]) / 2 });
    await devtools.send('Input.insertText', { text: 'Y' });
    let resetChanges;
    try {
      resetChanges = await until(() => devtools.evaluate('window.__presenceChanges'),
        values => values.at(-1)?.data?.name === 'Y', 'host reset before edit', 5_000);
    } catch (error) {
      const state = await devtools.evaluate(`(() => {
        const form=document.querySelector('#contact-form');
        return {data:form.getAttribute('data'),provided:form.getAttribute('data-provided'),
          changes:window.__presenceChanges};
      })()`);
      const tree = await devtools.send('Accessibility.getFullAXTree');
      throw new Error(`${error.message}: ${JSON.stringify({state,fields:tree.nodes
        .filter(node => node.role?.value === 'textbox')
        .map(node => ({name:node.name?.value,value:node.value?.value}))})}`);
    }
    assert.equal(resetChanges.length, reconnectChanges.length + 1);
    assert.equal(resetChanges.at(-1).dataProvided, true);
    assert.equal(await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      return form.shadowRoot===window.__retainedShadow && form.shadowRoot.childElementCount===2;
    })()`), true);

    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      window.__nameSchema=form.getAttribute('schema');
      window.__registryCalls=[];
      form.setAttribute('data',JSON.stringify({address:{street:'Main'}}));
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{
        address:{type:'object',properties:{street:{type:'string',title:'Street'}}}
      }}));
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/address'}));
      form.uiSchemaRegistry=[{
        tester:(schema,schemaPath,path) => {
          window.__registryCalls.push({schemaType:schema?.type,schemaPath,path});
          return schema?.type==='object' ? 10 : -1;
        },
        uischema:{type:'VerticalLayout',elements:[
          {type:'Label',text:'Direct registry detail'},
          {type:'Control',scope:'#/properties/street'}
        ]}
      }];
    })()`);
    const registryCalls = await until(() => devtools.evaluate('window.__registryCalls'),
      calls => calls.some(call => call.schemaType === 'object'),
      'direct UI schema tester invocation');
    assert.ok(registryCalls.some(call => call.path === 'address'));
    assert.ok(registryCalls.some(call => call.schemaPath === '#/properties/address'));
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value === 'Direct registry detail');
    }, Boolean, 'tester-selected object detail');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      const entries=form.uiSchemaRegistry;
      entries[0].tester=() => -1;
      form.uiSchemaRegistry=entries;
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return !tree.nodes.some(node => node.name?.value === 'Direct registry detail');
    }, Boolean, 'generated detail after same-array tester replacement');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      const entries=form.uiSchemaRegistry;
      entries[0].tester=() => 10;
      form.uiSchemaRegistry=entries;
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value === 'Direct registry detail');
    }, Boolean, 'registered detail after same-array tester restoration');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      window.__registryDiagnostics=[];
      form.addEventListener('jsonforms-diagnostic', event =>
        window.__registryDiagnostics.push(event.detail));
      const entries=form.uiSchemaRegistry;
      entries[0].tester=() => { throw new Error('tester failed'); };
      form.uiSchemaRegistry=entries;
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return !tree.nodes.some(node => node.name?.value === 'Direct registry detail');
    }, Boolean, 'generated detail after tester failure');
    const testerDiagnostics = await until(() => devtools.evaluate('window.__registryDiagnostics'),
      values => values.some(value => value.code === 'uischema.testerFailed'),
      'tester failure diagnostic');
    assert.ok(testerDiagnostics.some(value => value.path === 'address' &&
      value.message.includes('tester failed')));
    await devtools.evaluate("document.querySelector('#contact-form').uiSchemaRegistry=[]");
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return !tree.nodes.some(node => node.name?.value === 'Direct registry detail');
    }, Boolean, 'generated detail after registry replacement');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      window.__profileDiagnostics=[];
      window.__profileChangeCount=window.__presenceChanges.length;
      window.__nameConfig=form.getAttribute('config');
      form.addEventListener('jsonforms-diagnostic', event =>
        window.__profileDiagnostics.push(event.detail));
      form.setAttribute('data',JSON.stringify({source:'first'}));
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{source:{type:'string'}}}));
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/source',
        options:{format:'code',language:'json'}}));
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value === 'Monaco editor is unavailable on this host');
    }, Boolean, 'visible Monaco fallback');
    await until(() => devtools.evaluate('window.__profileDiagnostics'),
      values => values.some(value => value.code === 'editor.monacoUnavailable'),
      'Monaco fallback diagnostic');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      form.setAttribute('data',JSON.stringify({name:'Grace'}));
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{name:{type:'string'}}}));
      form.setAttribute('config',JSON.stringify({jsonformsExtended:{security:{allowScriptEvaluation:true}}}));
      form.setAttribute('uischema',JSON.stringify({type:'TemplateLayout',lang:'jsx',
        template:'<div>{elements}</div>',elements:[{type:'Control',scope:'#/properties/name'}]}));
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value ===
        'Template engine is unavailable on this host: jsx');
    }, Boolean, 'visible TemplateLayout fallback');
    await until(() => devtools.evaluate('window.__profileDiagnostics'),
      values => values.some(value => value.code === 'template.engineUnavailable'),
      'TemplateLayout fallback diagnostic');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      form.setAttribute('data',JSON.stringify({rows:['A']}));
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{rows:{type:'array',
        items:{type:'string'}}}}));
      form.setAttribute('config','{}');
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/rows',
        options:{variant:'ag-grid'}}));
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value ===
        'AG Grid is unavailable here; showing an array table.');
    }, Boolean, 'visible AG Grid table fallback');
    await until(() => devtools.evaluate('window.__profileDiagnostics'),
      values => values.some(value => value.code === 'variant.unsupported'),
      'AG Grid fallback diagnostic');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      form.setAttribute('renderer-family','cupertino');
      window.__profileDiagnostics=[];
      form.setAttribute('data',JSON.stringify({source:'first'}));
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{source:{type:'string'}}}));
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/source',
        options:{format:'code',language:'json'}}));
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value === 'Monaco editor is unavailable on this host');
    }, Boolean, 'visible Cupertino Monaco fallback');
    await until(() => devtools.evaluate('window.__profileDiagnostics'),
      values => values.some(value => value.code === 'editor.monacoUnavailable'),
      'Cupertino Monaco fallback diagnostic');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      window.__profileDiagnostics=[];
      form.setAttribute('data',JSON.stringify({name:'Grace'}));
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{name:{type:'string'}}}));
      form.setAttribute('config',JSON.stringify({jsonformsExtended:{security:{allowScriptEvaluation:true}}}));
      form.setAttribute('uischema',JSON.stringify({type:'TemplateLayout',lang:'jsx',
        template:'<div>{elements}</div>',elements:[{type:'Control',scope:'#/properties/name'}]}));
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value ===
        'Template engine is unavailable on this host: jsx');
    }, Boolean, 'visible Cupertino TemplateLayout fallback');
    await until(() => devtools.evaluate('window.__profileDiagnostics'),
      values => values.some(value => value.code === 'template.engineUnavailable'),
      'Cupertino TemplateLayout fallback diagnostic');
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      window.__profileDiagnostics=[];
      form.setAttribute('data',JSON.stringify({rows:['A']}));
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{rows:{type:'array',
        items:{type:'string'}}}}));
      form.setAttribute('config','{}');
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/rows',
        options:{variant:'ag-grid'}}));
    })()`);
    await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value ===
        'AG Grid is unavailable here; showing an array table.');
    }, Boolean, 'visible Cupertino AG Grid table fallback');
    await until(() => devtools.evaluate('window.__profileDiagnostics'),
      values => values.some(value => value.code === 'variant.unsupported'),
      'Cupertino AG Grid fallback diagnostic');
    assert.equal(await devtools.evaluate("document.querySelector('#contact-form').getAttribute('data')"),
      JSON.stringify({ rows: ['A'] }));
    assert.equal(await devtools.evaluate('window.__presenceChanges.length'),
      await devtools.evaluate('window.__profileChangeCount'));
    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      form.setAttribute('renderer-family','material');
      form.setAttribute('data',JSON.stringify({name:'Grace'}));
      form.setAttribute('schema',window.__nameSchema);
      form.setAttribute('config',window.__nameConfig);
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/name'}));
      form.removeAttribute('data-provided');
    })()`);

    await devtools.evaluate(`(() => {
      const form=document.querySelector('#contact-form');
      const schema=JSON.parse(form.getAttribute('schema'));
      schema.properties.name.enum=['Grace','Ada'];
      form.setAttribute('schema',JSON.stringify(schema));
    })()`);
    await sleep(250);
    const menuTree = await devtools.send('Accessibility.getFullAXTree');
    const selector = menuTree.nodes.find(node => node.role?.value === 'textbox' &&
      node.backendDOMNodeId);
    assert.ok(selector, 'enum selector is accessible');
    const { model: selectorModel } = await devtools.send('DOM.getBoxModel',
      { backendNodeId: selector.backendDOMNodeId });
    await devtools.click({ x: (selectorModel.border[0] + selectorModel.border[2]) / 2,
      y: (selectorModel.border[1] + selectorModel.border[5]) / 2 });
    const popupOption = await until(async () => {
      const tree = await devtools.send('Accessibility.getFullAXTree');
      return tree.nodes.find(node => node.name?.value === 'Ada' && node.backendDOMNodeId);
    }, Boolean, 'enum popup option', 10_000);
    const { object: optionObject } = await devtools.send('DOM.resolveNode',
      { backendNodeId: popupOption.backendDOMNodeId });
    const popupLocation = await devtools.send('Runtime.callFunctionOn', {
      objectId: optionObject.objectId,
      functionDeclaration: `function() {
        const hosts=[];
        let node=this;
        while (node) {
          const root=node.getRootNode();
          if (!root.host) break;
          hosts.push(root.host.tagName);
          node=root.host;
        }
        const style=getComputedStyle(this);
        return {hosts, display:style.display, color:style.color};
      }`, returnByValue: true,
    });
    assert.ok(popupLocation.result.value.hosts.includes('JSONFORMS-KOTLIN-COMPOSE'),
      `popup escaped component ShadowRoot: ${JSON.stringify(popupLocation.result.value)}`);
    assert.notEqual(popupLocation.result.value.display, 'none');
    assert.notEqual(popupLocation.result.value.color, 'rgba(0, 0, 0, 0)');
    const { model: optionModel } = await devtools.send('DOM.getBoxModel',
      { backendNodeId: popupOption.backendDOMNodeId });
    await devtools.click({ x: (optionModel.border[0] + optionModel.border[2]) / 2,
      y: (optionModel.border[1] + optionModel.border[5]) / 2 });
    const popupChanges = await until(() => devtools.evaluate('window.__presenceChanges'),
      values => values.some(change => change.data?.name === 'Ada'),
      'enum popup selection in ShadowRoot');
    assert.equal(popupChanges.at(-1).dataProvided, true);
    await devtools.send('Input.dispatchKeyEvent', {
      type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27,
    });
    await devtools.send('Input.dispatchKeyEvent', {
      type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27,
    });

    await devtools.evaluate(`(() => {
      const style=document.createElement('style');
      style.textContent='header,main section+section{display:none} main{display:block;max-width:none;margin:0;padding:0} main section{padding:0;border:0}';
      document.head.append(style);
    })()`);
    for (const height of [720, 240]) {
      await devtools.send('Emulation.setDeviceMetricsOverride', {
        width: 240, height, deviceScaleFactor: 1, mobile: false,
      });
      for (const family of ['material', 'cupertino']) {
        await devtools.evaluate(`(() => {
          const form=document.querySelector('#contact-form');
          const parent=form.parentNode;
          form.remove();
          form.setAttribute('renderer-family',${JSON.stringify(family)});
          form.setAttribute('dir','ltr');
          form.setAttribute('lang','en');
          form.setAttribute('data',JSON.stringify({duration:'PT1H30M'}));
          form.setAttribute('schema',JSON.stringify({type:'object',properties:{duration:{type:'string',format:'duration'}}}));
          form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/duration'}));
          form.style.minHeight=${JSON.stringify(height + 'px')};
          parent.appendChild(form);
        })()`);
        await sleep(500);
        const readViewportHeights = () => devtools.evaluate(`(() => {
          const form=document.querySelector('#contact-form');
          const host=form.shadowRoot.firstElementChild;
          return {form:form.getBoundingClientRect().height,
            host:host.getBoundingClientRect().height,
            compose:host.firstElementChild.getBoundingClientRect().height,
            hostStyle:host.getAttribute('style'),
            composeStyle:host.firstElementChild.getAttribute('style')};
        })()`);
        const viewportHeights = await until(readViewportHeights,
          heights => Math.abs(heights.form - heights.compose) <= 2,
          `${family} Compose resize after reconnect`, 5_000);
        assert.ok(Math.abs(viewportHeights.form - viewportHeights.compose) <= 2,
          `${family} Compose viewport does not fill ${height}px Web Component: ${JSON.stringify(viewportHeights)}`);
        const oldPopupTree = await devtools.send('Accessibility.getFullAXTree');
        const oldPopupIds = new Set(oldPopupTree.nodes.filter(node =>
          ['Years to seconds', 'Weeks', 'Remove Hours', 'Remove Minutes', 'Cancel', 'Apply']
            .includes(node.name?.value)).map(node => node.backendDOMNodeId));
        const rect = await devtools.evaluate(`(() => {
          const {x,y,width}=document.querySelector('#contact-form').getBoundingClientRect();
          return {x,y,width};
        })()`);
        await devtools.click({ x: rect.x + rect.width - (family === 'cupertino' ? 34 : 24),
          y: rect.y + (family === 'cupertino' ? 60 : 36) });
        const popupNodes = await until(async () => {
          const tree = await devtools.send('Accessibility.getFullAXTree');
          const names = ['Years to seconds', 'Weeks', 'Remove Hours', 'Remove Minutes', 'Cancel', 'Apply'];
          const nodes = names.map(name => tree.nodes.find(node =>
            node.name?.value === name && node.backendDOMNodeId));
          return nodes.every(node => node && !oldPopupIds.has(node.backendDOMNodeId)) ? nodes : null;
        }, Boolean, `${family} duration popup`, 10_000);
        const checkVisible = async nodes => {
          for (const node of nodes) {
            const model = await until(async () => {
              const { model } = await devtools.send('DOM.getBoxModel',
                { backendNodeId: node.backendDOMNodeId });
              return model.border[2] > model.border[0] ? model : null;
            }, Boolean, `${family} ${node.name.value} layout at ${height}`, 5_000);
            const centerY = (model.border[1] + model.border[5]) / 2;
            assert.ok(model.border[0] >= -2 && model.border[2] <= 242 &&
              centerY >= 0 && centerY <= height,
            `${family} duration popup action ${node.name.value} is unreachable at 240x${height}: ${model.border}`);
          }
        };
        if (height === 240) {
          await checkVisible(popupNodes.slice(0, 2));
          await devtools.send('Input.dispatchMouseEvent', {
            type: 'mouseWheel', x: 120, y: 180, deltaX: 0, deltaY: 320,
          });
          await sleep(300);
          await checkVisible(popupNodes.slice(2));
        } else {
          await checkVisible(popupNodes);
        }
        assert.ok(await devtools.evaluate('document.documentElement.scrollWidth <= innerWidth'),
          `${family} duration popup widens the document`);
        const apply = popupNodes.at(-1);
        const { model: applyModel } = await devtools.send('DOM.getBoxModel',
          { backendNodeId: apply.backendDOMNodeId });
        const changesBefore = await devtools.evaluate('window.__presenceChanges.length');
        await devtools.click({ x: (applyModel.border[0] + applyModel.border[2]) / 2,
          y: (applyModel.border[1] + applyModel.border[5]) / 2 });
        assert.equal(await devtools.evaluate('window.__presenceChanges.length'), changesBefore,
          `${family} untouched duration Apply wrote data`);
      }
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
