// Karma runs webpack from the repository root, while Kotlin installs its
// test loaders under build/js/node_modules.
for (const rule of config.module.rules) {
  if (rule.enforce === 'pre' && Array.isArray(rule.use)) {
    rule.use = rule.use.map((loader) =>
      loader === 'source-map-loader' ? require.resolve('source-map-loader') : loader,
    );
  }
}

// Okio constructs a browser file-system placeholder when the tests start.
config.resolve = config.resolve || {};
config.resolve.fallback = {
  ...config.resolve.fallback,
  os: require('path').resolve(__dirname, '../../../../scripts/browser-os.cjs'),
  path: false,
};
