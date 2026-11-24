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

for (const family of ['material', 'cupertino']) for (const locale of ['en', 'bg']) test(
  `${family} ${locale} color picker fits a 240 pixel Web Component`,
  { timeout: 90_000 }, async () => {
    const labels = locale === 'bg' ? {
      choose:'Избери цвят', spectrum:'Наситеност и яркост', rgb:'Канали RGB',
      red:'Червено канал', redValue:'Червено стойност',
      alpha:'Прозрачност', alphaValue:'Прозрачност стойност',
      hsb:'Канали HSB', hue:'Нюанс канал', hueValue:'Нюанс стойност',
    } : {
      choose:'Choose color', spectrum:'Saturation and brightness', rgb:'RGB channels',
      red:'Red channel', redValue:'Red channel value',
      alpha:'Alpha channel', alphaValue:'Alpha channel value',
      hsb:'HSB channels', hue:'Hue channel', hueValue:'Hue channel value',
    };
    await access(join(bundle, 'jsonforms-kotlin-webcomponent.js'));
    await access(chromePath);
    const html = `<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
      <style>html,body{margin:0;width:240px}jsonforms-kotlin-compose{display:block;width:240px;min-height:720px}</style>
      </head><body><jsonforms-kotlin-compose id="form" renderer-family="${family}" lang="${locale}"></jsonforms-kotlin-compose>
      <script>const form=document.querySelector('#form');
        form.setAttribute('schema',JSON.stringify({type:'object',properties:{shade:{type:'string',format:'color'}}}));
        form.setAttribute('data',JSON.stringify({shade:'#2255aa'}));
        form.setAttribute('uischema',JSON.stringify({type:'Control',scope:'#/properties/shade'}));
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
    const profile = await mkdtemp(join(tmpdir(), 'jsonforms-color-chrome-'));
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
      }, Boolean, 'color page');
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
          left:model.border[0], right:model.border[2],
          top:model.border[1], bottom:model.border[5] };
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
        Boolean, 'mounted color form');
      await sleep(500);
      const heights = await evaluate(`(() => {
        const form=document.querySelector('#form');
        return {form:form.getBoundingClientRect().height,
          compose:form.shadowRoot.firstElementChild.firstElementChild.getBoundingClientRect().height};
      })()`);
      assert.ok(Math.abs(heights.form - heights.compose) <= 2,
        `${family} Compose viewport does not fill host: ${JSON.stringify(heights)}`);
      await click(await visibleBox(await node(labels.choose)));
      const spectrum = await visibleBox(await node(labels.spectrum));
      assert.ok(spectrum.left >= -2 && spectrum.right <= 242,
        `${family} color spectrum escapes viewport: ${JSON.stringify(spectrum)}`);
      assert.equal(await evaluate('window.formChanges.length'), 0);
      await click(await visibleBox(await node(labels.rgb)));
      assert.equal(await evaluate('window.formChanges.length'), 0,
        `${family} switching color channels wrote data`);
      const redSlider = await visibleBox(await node(labels.red));
      const redValue = await visibleBox(await node(labels.redValue));
      assert.ok(redSlider.right - redSlider.left >= 48,
        `${family} Red slider is too narrow: ${JSON.stringify(redSlider)}`);
      assert.ok(redValue.left >= -2 && redValue.right <= 242,
        `${family} Red value escapes viewport: ${JSON.stringify(redValue)}`);
      assert.ok(redSlider.right <= redValue.left + 2 ||
        redValue.bottom <= redSlider.top + 2 || redSlider.bottom <= redValue.top + 2,
        `${family} Red slider overlaps its value field`);
      await click(spectrum);
      const changes = await until(() => evaluate('window.formChanges'),
        entries => entries.length > 0, `${family} spectrum edit`);
      assert.notEqual(changes.at(-1).data.shade, '#2255aa');
      await send('Input.dispatchMouseEvent', {
        type:'mouseWheel', x:120, y:540, deltaX:0, deltaY:700,
      });
      const alphaSlider = await visibleBox(await node(labels.alpha));
      const alphaValue = await visibleBox(await node(labels.alphaValue));
      assert.ok(alphaSlider.right - alphaSlider.left >= 48,
        `${family} Alpha slider is too narrow: ${JSON.stringify(alphaSlider)}`);
      assert.ok(alphaValue.left >= -2 && alphaValue.right <= 242,
        `${family} Alpha value escapes viewport: ${JSON.stringify(alphaValue)}`);
      assert.ok(alphaSlider.right <= alphaValue.left + 2 ||
        alphaValue.bottom <= alphaSlider.top + 2 || alphaSlider.bottom <= alphaValue.top + 2,
        `${family} Alpha slider overlaps its value field`);
      await send('Input.dispatchMouseEvent', {
        type:'mouseWheel', x:120, y:340, deltaX:0, deltaY:-900,
      });
      await sleep(300);
      await click(await visibleBox(await node(labels.hsb)));
      await sleep(200);
      const hueSlider = await visibleBox(await node(labels.hue));
      const hueValue = await visibleBox(await node(labels.hueValue));
      assert.ok(hueSlider.right - hueSlider.left >= 48,
        `${family} Hue slider is too narrow: ${JSON.stringify(hueSlider)}`);
      assert.ok(hueValue.left >= -2 && hueValue.right <= 242,
        `${family} Hue value escapes viewport: ${JSON.stringify(hueValue)}`);
      assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth'),
        `${family} color dialog widened the document`);
    } finally {
      socket?.close();
      chrome.kill('SIGTERM');
      if (chrome.exitCode === null && chrome.signalCode === null)
        await new Promise(done => chrome.once('exit', done));
      await rm(profile, { recursive:true, force:true });
      await new Promise(done => server.close(done));
    }
  });
