import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import { access, mkdtemp, readFile, rm } from 'node:fs/promises';
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

for (const rendererFamily of ['material', 'cupertino'])
for (const dialogKind of ['row', 'cell']) test(
  `${rendererFamily} long table ${dialogKind} dialog keeps footer visible after viewport shrinks`,
  { timeout: 120_000 }, async () => {
  await access(join(bundle, 'jsonforms-kotlin-webcomponent.js'));
  await access(chromePath);
  const html = `<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
    <style>html,body{margin:0;width:100%;height:100%}jsonforms-kotlin-compose{display:block;width:100%;height:100%}</style>
    </head><body><jsonforms-kotlin-compose id="form" renderer-family="${rendererFamily}"></jsonforms-kotlin-compose>
    <script>
      const fields=Object.fromEntries(Array.from({length:20},(_,i)=>['field'+i,{type:'string'}]));
      const values=Object.fromEntries(Object.keys(fields).map(key=>[key,key]));
      const mode=${JSON.stringify(dialogKind)};
      const row=mode==='row'?values:{contact:values};
      const properties=mode==='row'?fields:{contact:{type:'object',properties:fields}};
      const options=mode==='row'
        ? {table:true,columnDefs:[{field:'field0'}],rowDetail:{presentation:'dialog',dialog:{width:620}}}
        : {table:true,columnDefs:[{field:'contact'}],cells:{contact:{
            summary:{type:'Control',scope:'#/properties/field0'},
            detail:{type:'VerticalLayout',elements:Object.keys(fields).map(key=>({type:'Control',scope:'#/properties/'+key}))},
            dialog:{width:620}
          }}};
      const form=document.querySelector('#form');
      form.setAttribute('schema',JSON.stringify({type:'object',properties:{rows:{type:'array',items:{type:'object',properties}}}}));
      form.setAttribute('data',JSON.stringify({rows:[row]}));
      form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/rows',options}));
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
  const profile = await mkdtemp(join(tmpdir(), 'jsonforms-dialog-chrome-'));
  const chrome = spawn(chromePath, [
    '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
    '--disable-dev-shm-usage', '--window-size=900,700', '--remote-debugging-port=0',
    `--user-data-dir=${profile}`, address,
  ], { stdio:'ignore' });
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
      const result = new Promise(resolve => pending.set(id, resolve));
      socket.send(JSON.stringify({ id, method, params }));
      const message = await result;
      if (message.error) throw Error(`${method}: ${message.error.message}`);
      return message.result;
    }
    async function evaluate(expression) {
      const result = await send('Runtime.evaluate', { expression, returnByValue:true });
      if (result.exceptionDetails) throw Error(result.exceptionDetails.text);
      return result.result.value;
    }
    async function namedBounds(name, role) {
      const tree = await send('Accessibility.getFullAXTree');
      const node = tree.nodes.find(node => (!role || node.role?.value === role) &&
        node.name?.value === name && node.backendDOMNodeId);
      if (!node) return null;
      const { model } = await send('DOM.getBoxModel', { backendNodeId:node.backendDOMNodeId });
      return { left:model.border[0], top:model.border[1],
        right:model.border[4], bottom:model.border[5] };
    }
    const button = name => namedBounds(name, 'button');
    await send('Emulation.setDeviceMetricsOverride', { width:800, height:600, deviceScaleFactor:1, mobile:false });
    await until(() => evaluate("document.querySelector('#form')?._jsonformsMounted===true"), Boolean, 'mounted form');
    const editName = dialogKind === 'row' ? 'Edit details' : 'Edit Contact';
    const edit = await until(() => button(editName), Boolean, `${dialogKind} detail action`);
    for (const type of ['mousePressed', 'mouseReleased']) {
      await send('Input.dispatchMouseEvent', { type, x:(edit.left+edit.right)/2,
        y:(edit.top+edit.bottom)/2, button:'left', clickCount:1 });
    }
    await until(() => button('Apply'), rect => rect?.right > rect?.left &&
      rect?.bottom > rect?.top, 'initial dialog footer');
    await send('Emulation.setDeviceMetricsOverride', { width:360, height:260, deviceScaleFactor:1, mobile:false });
    await until(() => evaluate('window.innerHeight'), height => height === 260, 'short viewport');
    const footer = await until(() => button('Apply'),
      rect => rect && rect.right > rect.left && rect.bottom > rect.top &&
        rect.left >= 0 && rect.right <= 360 && rect.top >= 0 && rect.bottom <= 260,
      'clamped dialog footer', 5_000);
    const cancel = await button('Cancel');
    assert.ok(cancel, 'Cancel action remains available');
    const titleName = dialogKind === 'row' ? 'Item 1 details' : 'Contact details';
    const title = await namedBounds(titleName);
    assert.ok(title, 'Dialog title remains available');
    for (const [name, rect] of [['Title', title], ['Apply', footer], ['Cancel', cancel]]) {
      assert.ok(rect.right > rect.left && rect.bottom > rect.top,
        `${name} has no visible bounds: ${JSON.stringify(rect)}`);
      assert.ok(rect.left >= 0 && rect.right <= 360, `${name} exceeds viewport width: ${JSON.stringify(rect)}`);
      assert.ok(rect.top >= 0 && rect.bottom <= 260, `${name} exceeds viewport height: ${JSON.stringify(rect)}`);
    }
    const beforeScroll = await namedBounds('Field0', 'textbox');
    assert.ok(beforeScroll?.bottom > beforeScroll?.top, 'First field starts visible');
    await send('Input.dispatchMouseEvent', { type:'mouseWheel', x:180, y:130, deltaX:0, deltaY:850 });
    await until(async () => {
      for (let i = 3; i < 20; i++) {
        const rect = await namedBounds(`Field${i}`, 'textbox');
        if (rect && rect.bottom > rect.top && rect.top >= title.bottom &&
          rect.bottom <= footer.top) return i;
      }
      return null;
    }, Boolean, 'later field scrolled into the dialog body', 5_000);
    const afterScroll = await namedBounds('Field0', 'textbox');
    assert.ok(afterScroll?.bottom <= afterScroll?.top, 'First field scrolls out of view');
    const afterFooter = await button('Apply');
    const afterTitle = await namedBounds(titleName);
    assert.ok(Math.abs(afterFooter.top - footer.top) <= 2, 'Footer stays fixed during body scroll');
    assert.ok(Math.abs(afterTitle.top - title.top) <= 2, 'Title stays fixed during body scroll');
    assert.equal(await evaluate('window.scrollY'), 0, 'Page does not scroll with dialog body');
    assert.equal(await evaluate('window.formWrites'), 0);
  } finally {
    socket?.close();
    chrome.kill();
    await sleep(500);
    await rm(profile, { recursive:true, force:true, maxRetries:5, retryDelay:100 });
    await new Promise(resolve => server.close(resolve));
  }
});
