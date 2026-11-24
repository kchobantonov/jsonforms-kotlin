import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const jsDist = join(root, 'examples-compose/build/dist/js/productionExecutable');
const wasmDist = join(root, 'examples-compose/build/dist/wasmJs/productionExecutable');
const chromePath = process.env.CHROME_BIN ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const sitePrefix = '/jsonforms-kotlin';

async function until(read, matches, label, timeout = 60_000) {
  const start = Date.now();
  while (Date.now() - start < timeout) {
    const value = await read();
    if (matches(value)) return value;
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  throw new Error(`Timed out waiting for ${label}`);
}

function serveDistributions() {
  return createServer(async (request, response) => {
    const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
    if (!pathname.startsWith(`${sitePrefix}/`)) {
      response.writeHead(404).end();
      return;
    }
    const sitePath = pathname.slice(sitePrefix.length);
    const wasm = sitePath === '/wasm' || sitePath.startsWith('/wasm/');
    const base = wasm ? wasmDist : jsDist;
    const relative = (wasm ? sitePath.replace(/^\/wasm\/?/, '') : sitePath.slice(1)) || 'index.html';
    const file = resolve(base, relative);
    if (file !== base && !file.startsWith(`${base}${sep}`)) {
      response.writeHead(404).end();
      return;
    }
    try {
      const type = file.endsWith('.wasm') ? 'application/wasm'
        : file.endsWith('.js') || file.endsWith('.mjs') ? 'application/javascript'
          : file.endsWith('.svg') ? 'image/svg+xml' : 'text/html; charset=utf-8';
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
    const node = await until(() => this.namedNode(name), Boolean, `${name} action`, 10_000)
      .catch(async error => {
        const tree = await this.send('Accessibility.getFullAXTree');
        const names = tree.nodes.map(node => node.name?.value).filter(Boolean);
        throw new Error(`${error.message}; visible names: ${names.slice(0, 100).join(' | ')}`);
      });
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
}

test('JS and Wasm switch preserves working data and cleans one-time transfer',
  { timeout: 180_000 }, async () => {
    const server = serveDistributions();
    await new Promise((resolve, reject) => {
      server.once('error', reject);
      server.listen(0, '127.0.0.1', resolve);
    });
    const port = server.address().port;
    const profile = await mkdtemp(join(tmpdir(), 'jsonforms-runtime-switch-'));
    const chrome = spawn(chromePath, [
      '--headless=new', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
      '--disable-dev-shm-usage', '--window-size=1000,800', '--remote-debugging-port=0',
      `--user-data-dir=${profile}`, `http://127.0.0.1:${port}${sitePrefix}/#dynamic?drawer=false`,
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

      await devtools.click('Change data');
      await until(() => devtools.namedNode('aaa'), Boolean, 'edited JS form data');
      await devtools.click('Open settings');
      await until(() => devtools.namedNode('Close settings'), Boolean, 'JS Settings panel');
      await new Promise(resolve => setTimeout(resolve, 300));
      await devtools.clickAt(800, 200);
      await devtools.click('Kotlin/Wasm');
      await until(() => devtools.evaluate('location.pathname'),
        path => path === `${sitePrefix}/wasm/`, 'Wasm route');
      await until(() => devtools.namedNode('aaa'), Boolean, 'restored Wasm form data');
      assert.equal(await devtools.evaluate('location.hash'), '#dynamic?drawer=false');
      assert.equal(await devtools.evaluate('location.search'), '');
      assert.equal(await devtools.evaluate(
        "Object.keys(sessionStorage).filter(key => key.startsWith('jsonforms.demo.runtime.')).length"), 0);

      await devtools.click('Open settings');
      await until(() => devtools.namedNode('Close settings'), Boolean, 'Wasm Settings panel');
      await new Promise(resolve => setTimeout(resolve, 300));
      await devtools.clickAt(800, 200);
      await devtools.click('Kotlin/JS');
      await until(() => devtools.evaluate('location.pathname'),
        path => path === `${sitePrefix}/`, 'JS route');
      await until(() => devtools.namedNode('aaa'), Boolean, 'restored JS form data');
      assert.equal(await devtools.evaluate('location.hash'), '#dynamic?drawer=false');
      assert.equal(await devtools.evaluate('location.search'), '');

      await devtools.click('Go to welcome page');
      await until(() => devtools.namedNode('Choose a web runtime'), Boolean, 'welcome runtime choice');
      const welcomeTree = await devtools.send('Accessibility.getFullAXTree');
      const welcomeNames = welcomeTree.nodes.map(node => node.name?.value).filter(Boolean);
      assert.ok(welcomeNames.includes('JSON Forms Kotlin Material 3'),
        `Renderer title missing from accessibility tree: ${welcomeNames.slice(0, 100).join(' | ')}`);
      assert.ok(welcomeNames.includes('More Forms. Less Code.'),
        `Welcome tagline missing from accessibility tree: ${welcomeNames.slice(0, 100).join(' | ')}`);
      await devtools.click('Kotlin/Wasm');
      await until(() => devtools.evaluate('location.pathname'),
        path => path === `${sitePrefix}/wasm/`, 'Wasm welcome choice');
      assert.ok((await devtools.evaluate('location.hash')).startsWith('#'));
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
