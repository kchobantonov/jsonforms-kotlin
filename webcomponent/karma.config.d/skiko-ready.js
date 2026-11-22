const path = require('path');

if (__dirname.includes(`${path.sep}build${path.sep}js${path.sep}`)) {
  config.customLaunchers.ChromeHeadlessWebGL = {
    base: 'ChromeHeadless',
    flags: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-webgl', '--ignore-gpu-blocklist'],
  };
  config.browsers = ['ChromeHeadlessWebGL'];
  config.files.push({
    pattern: path.resolve(__dirname, '../../../../webcomponent/src/jsTest/resources/skiko-ready.js'),
    included: true,
    watched: false,
  });
}
