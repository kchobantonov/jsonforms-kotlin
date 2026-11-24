import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { access, mkdtemp, readFile, rm } from 'node:fs/promises';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const bundle = join(root, 'webcomponent/build/kotlin-webpack/js',
  process.env.WEBCOMPONENT_BUNDLE_MODE === 'production'
    ? 'productionExecutable' : 'developmentExecutable');
const chromePath = process.env.CHROME_BIN ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function until(read, predicate, label, timeout = 10_000) {
  const started = Date.now();
  while (Date.now() - started < timeout) {
    const value = await read();
    if (predicate(value)) return value;
    await sleep(100);
  }
  throw Error(`Timed out waiting for ${label}`);
}

for (const family of ['material', 'cupertino']) test(
  `${family} cron popup scrolls within a 240 pixel Web Component`,
  { timeout: 90_000 }, async () => {
    await access(join(bundle, 'jsonforms-kotlin-webcomponent.js'));
    await access(chromePath);
    const html = `<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
      <style>html,body{margin:0;width:240px}jsonforms-kotlin-compose{display:block;width:240px;min-height:720px}</style>
      </head><body><jsonforms-kotlin-compose id="form" renderer-family="${family}"></jsonforms-kotlin-compose>
      <script>const form=document.querySelector('#form');
        form.setAttribute('schema',JSON.stringify({type:'object',properties:{schedule:{type:'string',format:'cron'}}}));
        form.setAttribute('data',JSON.stringify({schedule:'0 0 9 * * *'}));
        form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/schedule'}));
        window.formChanges=[];
        form.addEventListener('jsonforms-change',event=>window.formChanges.push(event.detail));
      </script><script src="register.js"></script><script src="jsonforms-kotlin-webcomponent.js"></script>
      </body></html>`;
    const server = createServer(async (request, response) => {
      const path = new URL(request.url, 'http://localhost').pathname;
      if (path === '/') { response.writeHead(200, { 'content-type':'text/html' }).end(html); return; }
      const file = path === '/register.js'
        ? join(root, 'webcomponent/src/jsMain/resources/register.js')
        : path === '/jsonforms-kotlin-webcomponent.js' || /^\/[a-f0-9]+\.(?:wasm|js)$/.test(path)
          ? join(bundle, path.slice(1)) : null;
      if (!file) { response.writeHead(404).end(); return; }
      try {
        response.writeHead(200, { 'content-type':path.endsWith('.wasm')
          ? 'application/wasm' : 'application/javascript' }).end(await readFile(file));
      } catch { response.writeHead(404).end(); }
    });
    await new Promise((done, fail) => {
      server.once('error', fail);
      server.listen(0, '127.0.0.1', done);
    });
    const address = `http://127.0.0.1:${server.address().port}/`;
    const profile = await mkdtemp(join(tmpdir(), 'jsonforms-cron-chrome-'));
    const chrome = spawn(chromePath, [
      '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
      '--disable-dev-shm-usage', '--window-size=800,800', '--remote-debugging-port=0',
      `--user-data-dir=${profile}`, address,
    ], { stdio: 'ignore' });
    let socket;
    try {
      const port = await until(async () => {
        try { return (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
        catch { return null; }
      }, Boolean, 'Chrome DevTools');
      const page = await until(async () => {
        const tabs = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
        return tabs.find(tab => tab.type === 'page' && tab.url === address);
      }, Boolean, 'cron page');
      socket = new WebSocket(page.webSocketDebuggerUrl);
      await new Promise((done, fail) => { socket.onopen = done; socket.onerror = fail; });
      let nextId = 0;
      const pending = new Map();
      socket.onmessage = event => {
        const message = JSON.parse(event.data);
        pending.get(message.id)?.(message);
        pending.delete(message.id);
      };
      async function send(method, params = {}) {
        const id = ++nextId;
        const response = new Promise(done => pending.set(id, done));
        socket.send(JSON.stringify({ id, method, params }));
        const message = await response;
        if (message.error) throw Error(`${method}: ${message.error.message}`);
        return message.result;
      }
      async function evaluate(expression) {
        const result = await send('Runtime.evaluate', {
          expression, returnByValue:true, awaitPromise:true,
        });
        if (result.exceptionDetails) throw Error(result.exceptionDetails.text);
        return result.result.value;
      }
      async function node(name) {
        return until(async () => {
          const tree = await send('Accessibility.getFullAXTree');
          return tree.nodes.find(item => item.name?.value === name && item.backendDOMNodeId);
        }, Boolean, `${family} ${name}`);
      }
      async function box(item) {
        const { model } = await send('DOM.getBoxModel', { backendNodeId:item.backendDOMNodeId });
        return { x:(model.border[0]+model.border[2])/2, y:(model.border[1]+model.border[5])/2,
          left:model.border[0], right:model.border[2] };
      }
      async function visibleBox(item) {
        return until(async () => {
          const position = await box(item);
          return position.right > position.left && position.y > 0 && position.y <= 720
            ? position : null;
        }, Boolean, `${family} visible ${item.name?.value}`);
      }
      async function click(point) {
        for (const type of ['mousePressed', 'mouseReleased'])
          await send('Input.dispatchMouseEvent', {
            type, x:point.x, y:point.y, button:'left', clickCount:1,
          });
      }
      await send('Emulation.setDeviceMetricsOverride', {
        width:240, height:720, deviceScaleFactor:1, mobile:false,
      });
      await until(() => evaluate("document.querySelector('#form')?._jsonformsMounted===true"),
        Boolean, 'mounted cron form');
      await sleep(500);
      const heights = await evaluate(`(() => {
        const form=document.querySelector('#form');
        return {form:form.getBoundingClientRect().height,
          compose:form.shadowRoot.firstElementChild.firstElementChild.getBoundingClientRect().height};
      })()`);
      assert.ok(Math.abs(heights.form - heights.compose) <= 2,
        `${family} Compose viewport does not fill host: ${JSON.stringify(heights)}`);
      await click(await visibleBox(await node('Choose schedule')));
      const applyPoint = await visibleBox(await node('Apply'));
      const hours = await visibleBox(await node('Hours'));
      await send('Input.dispatchMouseEvent', {
        type:'mouseMoved', x:hours.x, y:hours.y,
      });
      const resetHours = await visibleBox(await node('Every: Hours'));
      await click(resetHours);
      assert.equal(await evaluate('window.formChanges.length'), 0,
        `${family} cron reset wrote before Apply`);
      const seconds = await visibleBox(await node('Seconds'));
      assert.ok(seconds.left >= -2 && seconds.right <= 242,
        `${family} Seconds selector escapes viewport: ${JSON.stringify(seconds)}`);
      await click(seconds);
      const first = await visibleBox(await node('Seconds: 0'));
      assert.ok(first.left >= -2 && first.right <= 242,
        `${family} cron menu escapes viewport: ${JSON.stringify(first)}`);
      const lastNode = await node('Seconds: 59');
      let last = null;
      for (let attempt = 0; attempt < 4 && !last; attempt++) {
        await send('Input.dispatchMouseEvent', {
          type:'mouseWheel', x:first.x, y:first.y + 100, deltaX:0, deltaY:1800,
        });
        await sleep(200);
        const position = await box(lastNode);
        if (position.right > position.left && position.y > 0 && position.y <= 720)
          last = position;
      }
      assert.ok(last, `${family} could not scroll to cron second 59`);
      assert.ok(last.left >= -2 && last.right <= 242,
        `${family} scrolled cron menu escapes viewport: ${JSON.stringify(last)}`);
      await click(last);
      assert.equal(await evaluate('window.formChanges.length'), 0,
        `${family} cron selection wrote before Apply`);
      await click({ x:20, y:110 });
      await sleep(300);
      await click(applyPoint);
      const changes = await until(() => evaluate('window.formChanges'),
        entries => entries.length > 0, `${family} cron Apply`);
      assert.equal(changes.at(-1).data.schedule, '0,59 0 * * * *');
      assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth'),
        `${family} cron popup widened the document`);
    } finally {
      socket?.close();
      chrome.kill('SIGTERM');
      if (chrome.exitCode === null && chrome.signalCode === null)
        await new Promise(done => chrome.once('exit', done));
      await rm(profile, { recursive:true, force:true });
      await new Promise(done => server.close(done));
    }
  });
