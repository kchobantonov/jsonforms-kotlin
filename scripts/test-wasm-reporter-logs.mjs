import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { test } from 'node:test';
import { runInNewContext } from 'node:vm';

const source = readFileSync(
  new URL('../examples-compose/karma.config.d/wasm-reporter-logs.js', import.meta.url),
  'utf8',
);
const key = 'reporter:karma-kotlin-reporter';

function configuredReporter(directory) {
  function OriginalReporter() {
    this.browserResults = {};
    this.checkBrowserResult = (browser) => {
      this.browserResults[browser.id] ??= { consoleResultCollector: {} };
    };
    this.specSuccess = (browser, result) => {
      this.events.push(['success', this.browserResults[browser.id]
        .consoleResultCollector[`${result.suite.join('.')}.${result.description}`]]);
    };
    this.specFailure = (browser, result) => {
      this.events.push(['failure', this.browserResults[browser.id]
        .consoleResultCollector[`${result.suite.join('.')}.${result.description}`]]);
    };
    this.events = [];
  }
  OriginalReporter.$inject = ['config'];
  const config = { plugins: [{ [key]: ['type', OriginalReporter] }] };
  runInNewContext(source, { __dirname: directory, config, require: (name) => {
    assert.equal(name, 'path');
    return path;
  } });
  return { OriginalReporter, Reporter: config.plugins[0][key][1] };
}

test('Wasm reporter handles missing completion logs on success and failure', () => {
  const { OriginalReporter, Reporter } = configuredReporter('/project/build/wasm/packages/test');
  assert.notEqual(Reporter, OriginalReporter);
  assert.deepEqual(Array.from(Reporter.$inject), ['config']);
  const reporter = new Reporter();
  const browser = { id: 'browser' };
  const success = { suite: ['form'], description: 'success' };
  const failure = { suite: ['form'], description: 'failure' };
  reporter.specSuccess(browser, success);
  reporter.specFailure(browser, failure);
  assert.deepEqual(reporter.events.map(([event, logs]) => [event, Array.from(logs)]), [
    ['success', []], ['failure', []],
  ]);
});

test('Wasm reporter retains existing logs and leaves JS reporter unchanged', () => {
  const { Reporter } = configuredReporter('/project/build/wasm/packages/test');
  const reporter = new Reporter();
  const browser = { id: 'browser' };
  const result = { suite: ['form'], description: 'logged' };
  reporter.checkBrowserResult(browser);
  reporter.browserResults.browser.consoleResultCollector['form.logged'] = ['existing log'];
  reporter.specSuccess(browser, result);
  assert.deepEqual(Array.from(reporter.events[0][1]), ['existing log']);

  const js = configuredReporter('/project/build/js/packages/test');
  assert.equal(js.Reporter, js.OriginalReporter);
});
