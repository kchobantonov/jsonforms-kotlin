// The Compose UI test runner creates its Skia surface synchronously. Wait for
// Skiko's asynchronous WASM setup before any mounted test enters that runner.
before(function () {
  this.timeout(60000);
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + 55000;
    function check() {
      try {
        if (window.GL) {
          // GL is a proxy whose getters become usable only after awaitSkiko.
          void window.GL.INVALID_ENUM;
          resolve();
          return;
        }
      } catch (_) {
        // The WASM module is still initializing.
      }
      if (Date.now() >= deadline) {
        reject(new Error('Skiko WASM did not initialize before UI tests'));
      } else {
        setTimeout(check, 20);
      }
    }
    check();
  });
});
