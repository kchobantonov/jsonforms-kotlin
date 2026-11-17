if (config.mode === 'production') {
  config.devtool = false;
  config.optimization = { ...config.optimization, minimize: false };
  config.module.rules = config.module.rules.filter((rule) =>
    !(rule.enforce === 'pre' && Array.isArray(rule.use) && rule.use.includes('source-map-loader')),
  );
}

// Keep Okio's browser file-system initialization usable in webcomponent bundles.
config.resolve = config.resolve || {};
config.resolve.fallback = {
  ...config.resolve.fallback,
  os: require('path').resolve(__dirname, '../../../../scripts/browser-os.cjs'),
  path: false,
};
