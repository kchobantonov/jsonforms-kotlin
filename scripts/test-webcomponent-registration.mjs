import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { runInNewContext } from 'node:vm';

const source = readFileSync(new URL('../webcomponent/src/jsMain/resources/register.js', import.meta.url), 'utf8');

function createElementClass(mount, matchMedia) {
  const browser = { jsonFormsKotlinElementFactory: undefined, matchMedia };
  runInNewContext(source, { window: browser, HTMLElement: class {} });
  return browser.jsonFormsKotlinElementFactory(mount);
}

function createThemeHarness() {
  let dark = false;
  const listeners = new Set();
  const query = {
    get matches() { return dark; },
    addEventListener(type, listener) { if (type === 'change') listeners.add(listener); },
    removeEventListener(type, listener) { if (type === 'change') listeners.delete(listener); },
  };
  const browser = { matchMedia: () => query };
  runInNewContext(source, { window: browser, HTMLElement: class {} });
  return {
    browser,
    setDark(value) { dark = value; for (const listener of listeners) listener(); },
    get listenerCount() { return listeners.size; },
  };
}

function createActionDispatcher() {
  const browser = {};
  class TestCustomEvent {
    constructor(type, options) {
      this.type = type;
      this.detail = options.detail;
      this.bubbles = options.bubbles;
    }
  }
  runInNewContext(source, { window: browser, HTMLElement: class {}, CustomEvent: TestCustomEvent });
  return browser.jsonFormsKotlinDispatchAction;
}

function createScriptExecutor() {
  const browser = {};
  class TestCustomEvent {
    constructor(type, options) { this.type = type; this.detail = options.detail; this.bubbles = options.bubbles; }
  }
  runInNewContext(source, { window: browser, HTMLElement: class {}, CustomEvent: TestCustomEvent });
  return browser.jsonFormsKotlinExecuteScript;
}

test('connected element mounts once and refreshes when reconnected', () => {
  let mounts = 0;
  let refreshes = 0;
  const connections = [];
  const Element = createElementClass(element => {
    mounts++;
    element._jsonformsRefresh = () => { refreshes++; };
    element._jsonformsConnectionChange = connected => connections.push(connected);
  });
  const element = new Element();

  element.isConnected = true;
  element.connectedCallback();
  assert.equal(mounts, 1);
  assert.equal(refreshes, 0);
  assert.deepEqual(connections, [true]);

  element.isConnected = false;
  element.disconnectedCallback();
  assert.deepEqual(connections, [true, false]);
  element.attributeChangedCallback('data', 'null', 'null');
  assert.equal(refreshes, 0);
  assert.equal(element._jsonformsDataChangedWhileDetached, true);

  element.isConnected = true;
  element.connectedCallback();
  assert.equal(mounts, 1);
  assert.equal(refreshes, 1);
  assert.deepEqual(connections, [true, false, true]);
});

test('connected attributes including direction refresh the mounted form', () => {
  let refreshes = 0;
  const changedAttributes = [];
  const Element = createElementClass(element => {
    element._jsonformsRefresh = name => { refreshes++; changedAttributes.push(name); };
  });
  const element = new Element();
  element.isConnected = true;
  element.connectedCallback();
  element.attributeChangedCallback();
  assert.equal(refreshes, 1);
  assert.ok(Element.observedAttributes.includes('config'));
  assert.ok(Element.observedAttributes.includes('dir'));
  for (const attribute of ['uischemas', 'translations', 'context', 'lang', 'theme', 'renderer-family',
    'data-provided', 'validation-mode', 'additional-errors']) {
    assert.ok(Element.observedAttributes.includes(attribute));
  }
  element.attributeChangedCallback('dir', 'ltr', 'rtl');
  assert.equal(refreshes, 2);
  element.attributeChangedCallback('data', 'null', 'null');
  assert.deepEqual(changedAttributes, [undefined, 'dir', 'data']);
});

test('direct UI schema registry property keeps tester functions and refreshes the form', () => {
  let refreshes = 0;
  let mountedRegistry;
  const Element = createElementClass(element => {
    mountedRegistry = element.uiSchemaRegistry;
    element._jsonformsRefresh = () => { refreshes++; };
  });
  const element = new Element();
  const tester = () => 10;
  const entries = [{ tester, uischema: { type: 'Label', text: 'From property' } }];
  element.uiSchemaRegistry = entries;
  element.isConnected = true;
  element.connectedCallback();
  assert.equal(mountedRegistry, entries);
  assert.equal(element.uiSchemaRegistry[0].tester, tester);
  assert.equal(refreshes, 0);
  element.uiSchemaRegistry = [];
  assert.equal(refreshes, 1);
  assert.equal(element.uiSchemaRegistry.length, 0);
});

test('registry property assigned before upgrade keeps its live setter', () => {
  let refreshes = 0;
  let mountedRegistry;
  const Element = createElementClass(element => {
    mountedRegistry = element.uiSchemaRegistry;
    element._jsonformsRegistryRefresh = () => { refreshes++; };
  });
  const element = new Element();
  const early = [{ tester: () => 10, uischema: { type: 'Label', text: 'Early' } }];
  Object.defineProperty(element, 'uiSchemaRegistry', {
    configurable: true, writable: true, value: early,
  });
  element.isConnected = true;
  element.connectedCallback();
  assert.equal(mountedRegistry, early);
  assert.equal(Object.prototype.hasOwnProperty.call(element, 'uiSchemaRegistry'), false);
  const replacement = [{ tester: () => 5, uischema: { type: 'Label', text: 'Later' } }];
  element.uiSchemaRegistry = replacement;
  assert.equal(element.uiSchemaRegistry, replacement);
  assert.equal(refreshes, 1);
});

test('system theme follows media changes without overriding explicit themes', () => {
  const harness = createThemeHarness();
  const resolve = harness.browser.jsonFormsKotlinResolvedTheme;
  assert.equal(resolve('system'), 'light');
  assert.equal(resolve(undefined), 'light');
  assert.equal(resolve('light'), 'light');
  assert.equal(resolve('dark'), 'dark');
  harness.setDark(true);
  assert.equal(resolve('system'), 'dark');
  assert.equal(resolve(undefined), 'dark');
  assert.equal(resolve('light'), 'light');
  assert.equal(resolve('dark'), 'dark');
});

test('color scheme listener refreshes a mounted form and detaches on disconnect', () => {
  const harness = createThemeHarness();
  let mounts = 0;
  let refreshes = 0;
  const Element = harness.browser.jsonFormsKotlinElementFactory(element => {
    mounts++;
    element._jsonformsRefresh = () => { refreshes++; };
  });
  const element = new Element();
  element.isConnected = true;
  element.connectedCallback();
  assert.equal(harness.listenerCount, 1);
  harness.setDark(true);
  assert.equal(refreshes, 1);
  element.isConnected = false;
  element.disconnectedCallback();
  assert.equal(harness.listenerCount, 0);
  harness.setDark(false);
  assert.equal(refreshes, 1);
  element.isConnected = true;
  element.connectedCallback();
  assert.equal(mounts, 1);
  assert.equal(refreshes, 2);
  assert.equal(harness.listenerCount, 1);
  harness.setDark(true);
  assert.equal(refreshes, 3);
});

test('action event waits for host promises and keeps action details', async () => {
  const dispatch = createActionDispatcher();
  let complete;
  const work = new Promise(resolve => { complete = resolve; });
  let event;
  const element = { dispatchEvent(value) {
    event = value;
    value.detail.waitUntil(work);
    value.detail.waitUntil(Promise.resolve());
  } };
  let settled = false;
  const pending = dispatch(element, 'save', { row: 3 }).then(() => { settled = true; });
  await Promise.resolve();
  assert.equal(settled, false);
  assert.equal(event.type, 'jsonforms-action');
  assert.equal(event.bubbles, true);
  assert.equal(event.detail.action, 'save');
  assert.equal(event.detail.params.row, 3);
  assert.throws(() => event.detail.waitUntil(Promise.resolve()), /while handling/);
  complete();
  await pending;
  assert.equal(settled, true);
});

test('action event propagates rejected host work', async () => {
  const dispatch = createActionDispatcher();
  const failure = new Error('save failed');
  const element = { dispatchEvent(event) {
    event.detail.waitUntil(Promise.reject(failure));
  } };
  await assert.rejects(dispatch(element, 'save', {}), error => error === failure);
});

test('action event waits for other host work before propagating a rejection', async () => {
  const dispatch = createActionDispatcher();
  const failure = new Error('first task failed');
  let finishOther;
  const other = new Promise(resolve => { finishOther = resolve; });
  const element = { dispatchEvent(event) {
    event.detail.waitUntil(Promise.reject(failure));
    event.detail.waitUntil(other);
  } };
  let settled = false;
  const completion = dispatch(element, 'save', {}).finally(() => { settled = true; });
  await Promise.resolve();
  await Promise.resolve();
  assert.equal(settled, false);
  finishOther();
  await assert.rejects(completion, error => error === failure);
  assert.equal(settled, true);
});

test('action event preserves triggering element and form context', async () => {
  const dispatch = createActionDispatcher();
  let detail;
  const metadata = {
    label: 'Save',
    path: 'orders.2',
    element: { type: 'Button', action: 'save', params: { row: 2 } },
    context: { data: { orders: [{ id: 1 }] }, schema: { type: 'object' }, locale: 'bg-BG', readonly: false },
  };
  await dispatch({ dispatchEvent(event) { detail = event.detail; } }, 'save', { row: 2 }, metadata);
  assert.equal(detail.action, 'save');
  assert.equal(detail.label, 'Save');
  assert.equal(detail.path, 'orders.2');
  assert.deepEqual(detail.element, metadata.element);
  assert.deepEqual(detail.context, metadata.context);
});

test('script executor awaits an async body with the event as this', async () => {
  const execute = createScriptExecutor();
  const host = { result: null, dispatchEvent() {} };
  const metadata = { label: 'Run', params: { value: 42 }, element: { type: 'Button' },
    context: { locale: 'bg-BG' } };
  await execute(host,
    'await Promise.resolve(); this.$el.result = [this.action, this.params.value, this.context.locale, this.element.type]',
    metadata);
  assert.deepEqual(Array.from(host.result), ['', 42, 'bg-BG', 'Button']);
});

test('script syntax failures emit a diagnostic and propagate', async () => {
  const execute = createScriptExecutor();
  let diagnostic;
  const host = { dispatchEvent(event) { diagnostic = event; } };
  assert.throws(() => execute(host, 'await (', { params: {} }), error => error.name === 'SyntaxError');
  assert.equal(diagnostic.type, 'jsonforms-diagnostic');
  assert.equal(diagnostic.detail.code, 'button.scriptCompileFailed');
  assert.equal(diagnostic.bubbles, true);
});
