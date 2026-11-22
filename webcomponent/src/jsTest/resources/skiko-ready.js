before(function () {
  this.timeout(60000);
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + 55000;
    function check() {
      try {
        if (window.GL) {
          void window.GL.INVALID_ENUM;
          resolve();
          return;
        }
      } catch (_) {
        // Skiko's asynchronous WASM setup has not completed.
      }
      if (Date.now() >= deadline) {
        reject(new Error('Skiko WASM did not initialize before DOM island test'));
      } else {
        setTimeout(check, 20);
      }
    }
    check();
  });
});
