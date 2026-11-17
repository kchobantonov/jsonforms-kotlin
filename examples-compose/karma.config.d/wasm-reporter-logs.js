// The Kotlin Karma reporter assumes every spec has emitted its console-completion marker.
// Skiko browser tests can finish without one, so provide an empty log for that spec.
// Keep the reporter's normal success/failure handling and Gradle test events intact.
const reporterPath = require('path');

if (__dirname.includes(`${reporterPath.sep}build${reporterPath.sep}wasm${reporterPath.sep}`)) {
  const key = 'reporter:karma-kotlin-reporter';
  const index = config.plugins.findIndex((plugin) => plugin && plugin[key]);
  if (index >= 0) {
    const plugin = config.plugins[index];
    const OriginalReporter = plugin[key][1];
    function ReporterWithOptionalConsoleLog(...args) {
      OriginalReporter.apply(this, args);
      for (const event of ['specSuccess', 'specFailure']) {
        const original = this[event];
        this[event] = (browser, result) => {
          this.checkBrowserResult(browser);
          const logs = this.browserResults[browser.id].consoleResultCollector;
          const spec = `${result.suite.join('.')}.${result.description}`;
          if (logs[spec] === undefined) logs[spec] = [];
          return original.call(this, browser, result);
        };
      }
    }
    ReporterWithOptionalConsoleLog.$inject = OriginalReporter.$inject;
    config.plugins[index] = {
      ...plugin,
      [key]: ['type', ReporterWithOptionalConsoleLog],
    };
  }
}
