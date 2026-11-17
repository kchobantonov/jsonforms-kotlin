if (config.mode === 'production') {
  config.devtool = false;
  config.optimization = { ...config.optimization, minimize: false };
  config.module.rules = config.module.rules.filter((rule) =>
    !(rule.enforce === 'pre' && Array.isArray(rule.use) && rule.use.includes('source-map-loader')),
  );
}

// Okio calls os.tmpdir() while constructing its browser file-system placeholder.
// Webpack's empty-module fallback does not provide that function.
config.resolve = config.resolve || {};
config.resolve.fallback = {
  ...config.resolve.fallback,
  os: require('path').resolve(__dirname, '../../../scripts/browser-os.cjs'),
  path: false,
};
