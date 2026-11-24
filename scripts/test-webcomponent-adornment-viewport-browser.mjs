import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import { readFile, mkdtemp, rm, access } from 'node:fs/promises';
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

async function until(read, predicate, label, timeout = 30_000) {
  const start = Date.now();
  while (Date.now() - start < timeout) {
    const value = await read();
    if (predicate(value)) return value;
    await sleep(100);
  }
  throw Error(`Timed out waiting for ${label}`);
}

for (const rendererFamily of ['material', 'cupertino']) test(
  `${rendererFamily} adornment tooltip fits a 240 pixel browser viewport`,
  { timeout: 120_000 }, async () => {
  await access(join(bundle, 'jsonforms-kotlin-webcomponent.js'));
  await access(chromePath);
  const html = `<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
    <style>html,body{margin:0;width:240px}jsonforms-kotlin-compose{display:block;width:240px;min-height:300px}</style>
    </head><body><jsonforms-kotlin-compose id="form" renderer-family="${rendererFamily}"></jsonforms-kotlin-compose>
    <script>const form=document.querySelector('#form');
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{value:{type:'string'}}}));
      form.setAttribute('data',JSON.stringify({value:'42'}));
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/value',options:{
        startAdornment:{kind:'text',text:'Unit',tooltip:'This unit explanation stays readable in a narrow input column.'}
      }}));
      window.formWrites=0;form.addEventListener('jsonforms-change',()=>window.formWrites++);
    </script><script src="register.js"></script><script src="jsonforms-kotlin-webcomponent.js"></script>
    </body></html>`;
  const server = createServer(async (request, response) => {
    const path = new URL(request.url, 'http://localhost').pathname;
    if (path === '/') { response.writeHead(200, {'content-type':'text/html'}).end(html); return; }
    const file = path === '/register.js'
      ? join(root, 'webcomponent/src/jsMain/resources/register.js')
      : path === '/jsonforms-kotlin-webcomponent.js' || /^\/[a-f0-9]+\.(?:wasm|js)$/.test(path)
        ? join(bundle, path.slice(1)) : null;
    if (!file) { response.writeHead(404).end(); return; }
    try { response.writeHead(200, {'content-type':path.endsWith('.wasm')?'application/wasm':'application/javascript'}).end(await readFile(file)); }
    catch { response.writeHead(404).end(); }
  });
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(0, '127.0.0.1', resolve); });
  const address = `http://127.0.0.1:${server.address().port}/`;
  const profile = await mkdtemp(join(tmpdir(), 'jsonforms-tooltip-chrome-'));
  const chrome = spawn(chromePath, [
    '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
    '--disable-dev-shm-usage', '--window-size=800,600', '--remote-debugging-port=0',
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
    }, Boolean, 'Web Component page');
    socket = new WebSocket(page.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    let nextId = 0;
    const pending = new Map();
    socket.onmessage = event => {
      const message = JSON.parse(event.data);
      pending.get(message.id)?.(message);
      pending.delete(message.id);
    };
    async function send(method, params = {}) {
      const id = ++nextId;
      const response = new Promise(resolve => pending.set(id, resolve));
      socket.send(JSON.stringify({ id, method, params }));
      const message = await response;
      if (message.error) throw Error(`${method}: ${message.error.message}`);
      return message.result;
    }
    async function evaluate(expression) {
      const result = await send('Runtime.evaluate', { expression, returnByValue:true, awaitPromise:true });
      if (result.exceptionDetails) throw Error(result.exceptionDetails.text);
      return result.result.value;
    }
    await send('Emulation.setDeviceMetricsOverride', { width:240, height:600, deviceScaleFactor:1, mobile:false });
    await until(() => evaluate('window.innerWidth'), width => width === 240, '240 pixel viewport');
    await until(() => evaluate("document.querySelector('#form')?._jsonformsMounted===true"), Boolean, 'mounted form');
    await sleep(500);
    const geometry = await evaluate(`({width:innerWidth, scrollWidth:document.documentElement.scrollWidth,
      form:document.querySelector('#form').getBoundingClientRect().toJSON()})`);
    const cupertinoBaseline = rendererFamily === 'cupertino'
      ? (await send('Page.captureScreenshot', { format:'png' })).data : null;
    const anchor = await until(async () => {
      const tree = await send('Accessibility.getFullAXTree');
      return tree.nodes.find(node => node.name?.value === 'Unit' && node.backendDOMNodeId);
    }, Boolean, 'adornment accessibility node');
    const { model } = await send('DOM.getBoxModel', { backendNodeId:anchor.backendDOMNodeId });
    const x = (model.border[0] + model.border[2]) / 2;
    const y = (model.border[1] + model.border[5]) / 2;
    await send('Input.dispatchMouseEvent', { type:'mouseMoved', x, y });
    const tooltip = await until(async () => {
      const tree = await send('Accessibility.getFullAXTree');
      return tree.nodes.find(node => node.name?.value ===
        'This unit explanation stays readable in a narrow input column.' && node.backendDOMNodeId);
    }, Boolean, 'tooltip accessibility node');
    const tree = await send('Accessibility.getFullAXTree');
    const parent = tree.nodes.find(node => node.nodeId === tooltip.parentId);
    assert.ok(parent?.backendDOMNodeId, 'Tooltip surface has an accessibility parent');
    const { model: parentModel } = await send('DOM.getBoxModel', { backendNodeId:parent.backendDOMNodeId });
    const xs = [parentModel.border[0], parentModel.border[2], parentModel.border[4], parentModel.border[6]];
    assert.ok(Math.min(...xs) >= -1, `Tooltip surface extends left of viewport: ${xs}; ${JSON.stringify(geometry)}`);
    assert.ok(Math.max(...xs) <= 241, `Tooltip surface extends right of viewport: ${xs}; ${JSON.stringify(geometry)}`);
    const scrollWidth = await evaluate('document.documentElement.scrollWidth');
    assert.equal(scrollWidth, geometry.scrollWidth, 'Tooltip changed document scroll width');
    assert.ok(scrollWidth <= 240, `Tooltip caused horizontal scrolling: ${scrollWidth}px`);
    assert.equal(await evaluate('window.formWrites'), 0);
    if (rendererFamily === 'cupertino') {
      await send('Input.dispatchMouseEvent', { type:'mouseMoved', x:225, y:250 });
      await sleep(700);
      await evaluate("document.querySelector('#form').setAttribute('renderer-family','material')");
      await until(async () => (await send('Page.captureScreenshot', { format:'png' })).data,
        image => image !== cupertinoBaseline, 'live Material renderer switch');
      assert.equal(await evaluate("document.querySelector('#form').getAttribute('data')"),
        '{"value":"42"}');
      assert.equal(await evaluate('window.formWrites'), 0);
      await evaluate("document.querySelector('#form').setAttribute('renderer-family','cupertino')");
      await until(async () => (await send('Page.captureScreenshot', { format:'png' })).data,
        image => image === cupertinoBaseline, 'restored Cupertino renderer');
    }
  } finally {
    socket?.close();
    chrome.kill();
    await sleep(500);
    await rm(profile, { recursive:true, force:true, maxRetries:5, retryDelay:100 });
    await new Promise(resolve => server.close(resolve));
  }
});
