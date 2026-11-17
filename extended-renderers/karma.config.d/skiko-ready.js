const path = require('path');

if (__dirname.includes(`${path.sep}build${path.sep}js${path.sep}`)) {
  config.files.push({
    pattern: path.resolve(__dirname, '../../../../extended-renderers/src/jsTest/resources/skiko-ready.js'),
    included: true,
    watched: false,
  });
}
