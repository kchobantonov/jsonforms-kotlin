if (config.mode === 'production') {
  config.devtool = false;
  config.optimization = { ...config.optimization, minimize: false };
  config.module.rules = config.module.rules.filter((rule) =>
    !(rule.enforce === 'pre' && Array.isArray(rule.use) && rule.use.includes('source-map-loader')),
  );
}
