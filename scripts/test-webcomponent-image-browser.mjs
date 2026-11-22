import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import { readFile, mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const resources = join(root, 'webcomponent/src/jsMain/resources');
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

function findCover(node) {
  if (!node || typeof node !== 'object') return null;
  if (node.cover?.type === 'ImageView') return node.cover;
  for (const value of Object.values(node)) {
    const found = Array.isArray(value) ? value.map(findCover).find(Boolean) : findCover(value);
    if (found) return found;
  }
  return null;
}

test('mounted Web Component decodes the imported Group cover image', { timeout: 120_000 }, async () => {
  const fixture = JSON.parse(await readFile(join(root, 'examples/spec/group-layout/uischema.json'), 'utf8'));
  const cover = findCover(fixture);
  assert.equal(cover?.options?.width, '100%');
  const server = createServer(async (request, response) => {
    const path = new URL(request.url, 'http://localhost').pathname;
    const files = {
      '/': [join(resources, 'index.html'), 'text/html'],
      '/register.js': [join(resources, 'register.js'), 'application/javascript'],
      '/jsonforms-kotlin-webcomponent.js': [join(bundle, 'jsonforms-kotlin-webcomponent.js'), 'application/javascript'],
    };
    const entry = files[path] ?? (/^\/[a-f0-9]+\.(?:wasm|js)$/.test(path)
      ? [join(bundle, path.slice(1)), path.endsWith('.wasm') ? 'application/wasm' : 'application/javascript']
      : null);
    if (!entry) { response.writeHead(404).end(); return; }
    try { response.writeHead(200, { 'content-type': entry[1] }).end(await readFile(entry[0])); }
    catch { response.writeHead(404).end(); }
  });
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  const address = `http://127.0.0.1:${server.address().port}/`;
  const profile = await mkdtemp(join(tmpdir(), 'jsonforms-image-chrome-'));
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
      const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
      if (result.exceptionDetails) throw Error(result.exceptionDetails.text);
      return result.result.value;
    }
    await until(() => evaluate("document.querySelector('#contact-form')?._jsonformsMounted === true"),
      Boolean, 'mounted form');
    const baseline = await send('Page.captureScreenshot', { format: 'png' });
    const ui = { type: 'Group', label: 'Card probe', cover, elements: [] };
    await evaluate(`(() => {
      const form = document.querySelector('#contact-form');
      form.setAttribute('schema', JSON.stringify({type:'object', properties:{}}));
      form.setAttribute('data', '{}');
      form.setAttribute('config', JSON.stringify({jsonformsExtended:{security:{urlPolicy:{allowImageDataUrls:true}}}}));
      form.setAttribute('uischema', ${JSON.stringify(JSON.stringify(ui))});
    })()`);
    await until(async () => {
      const tree = await send('Accessibility.getFullAXTree');
      return tree.nodes.some(node => node.name?.value === cover.alt);
    }, Boolean, 'cover alternative text');
    const tree = await send('Accessibility.getFullAXTree');
    const imageNode = tree.nodes.find(node => node.name?.value === cover.alt);
    assert.ok(imageNode?.backendDOMNodeId);
    const { model } = await send('DOM.getBoxModel', { backendNodeId: imageNode.backendDOMNodeId });
    const bounds = model.border;
    const image = await until(async () => {
      const shot = await send('Page.captureScreenshot', { format: 'png' });
      return shot.data.length > baseline.data.length + 50_000 ? shot : null;
    }, Boolean, 'decoded cover pixels');
    assert.ok(image.data.length > baseline.data.length + 50_000);
    const distinctColors = await evaluate(`new Promise(resolve => {
      const image = new Image();
      image.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = image.width; canvas.height = image.height;
        const context = canvas.getContext('2d');
        context.drawImage(image, 0, 0);
        const pixels = context.getImageData(0, 0, canvas.width, canvas.height).data;
        const box = ${JSON.stringify(bounds)};
        const left = Math.max(0, Math.floor(Math.min(box[0], box[2])));
        const right = Math.min(canvas.width, Math.ceil(Math.max(box[0], box[2])));
        const top = Math.max(0, Math.floor(Math.min(box[1], box[5])));
        const bottom = Math.min(canvas.height, Math.ceil(Math.max(box[1], box[5])));
        const colors = new Set();
        for (let y = top + Math.floor((bottom-top)*0.2); y < top + Math.floor((bottom-top)*0.8); y += 4) {
          for (let x = left + Math.floor((right-left)*0.25); x < left + Math.floor((right-left)*0.75); x += 4) {
            const index = (y * canvas.width + x) * 4;
            colors.add((pixels[index] << 16) | (pixels[index+1] << 8) | pixels[index+2]);
          }
        }
        resolve(colors.size);
      };
      image.onerror = () => resolve(0);
      image.src = ${JSON.stringify(`data:image/png;base64,${image.data}`)};
    })`);
    assert.ok(distinctColors > 100, `Expected decoded cover pixels, found ${distinctColors} colors`);
  } finally {
    socket?.close();
    chrome.kill();
    await sleep(500);
    await rm(profile, { recursive: true, force: true, maxRetries: 5, retryDelay: 100 });
    await new Promise(resolve => server.close(resolve));
  }
});
