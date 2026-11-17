window.jsonFormsKotlinElementFactory = (mount) => class JsonFormsKotlinElement extends HTMLElement {
  static get observedAttributes() {
    return ['data', 'schema', 'uischema', 'uischemas', 'config', 'context', 'translations', 'additional-errors',
      'readonly', 'data-provided', 'validation-mode', 'dir', 'lang', 'theme'];
  }

  get uiSchemaRegistry() {
    return this._jsonformsUiSchemaRegistry ?? (this._jsonformsUiSchemaRegistry = []);
  }

  set uiSchemaRegistry(entries) {
    this._jsonformsUiSchemaRegistry = entries;
    if (this.isConnected) (this._jsonformsRegistryRefresh ?? this._jsonformsRefresh)?.();
  }

  connectedCallback() {
    if (Object.prototype.hasOwnProperty.call(this, 'uiSchemaRegistry')) {
      const entries = this.uiSchemaRegistry;
      delete this.uiSchemaRegistry;
      this.uiSchemaRegistry = entries;
    }
    if (!this._jsonformsMounted) {
      this._jsonformsMounted = true;
      this._jsonformsDataChangedWhileDetached = false;
      this._jsonformsDataProvidedChangedWhileDetached = false;
      mount(this);
    } else if (this._jsonformsRefresh) {
      this._jsonformsRefresh();
    }
    if (!this._jsonformsColorSchemeQuery && window.matchMedia) {
      const query = window.matchMedia('(prefers-color-scheme: dark)');
      const refresh = () => this._jsonformsRefresh?.();
      query.addEventListener('change', refresh);
      this._jsonformsColorSchemeQuery = query;
      this._jsonformsColorSchemeRefresh = refresh;
    }
  }

  disconnectedCallback() {
    if (this._jsonformsColorSchemeQuery) {
      this._jsonformsColorSchemeQuery.removeEventListener('change', this._jsonformsColorSchemeRefresh);
      this._jsonformsColorSchemeQuery = null;
      this._jsonformsColorSchemeRefresh = null;
    }
  }

  attributeChangedCallback(name) {
    if (!this.isConnected) {
      if (name === 'data') this._jsonformsDataChangedWhileDetached = true;
      if (name === 'data-provided') this._jsonformsDataProvidedChangedWhileDetached = true;
      return;
    }
    if (this._jsonformsRefresh) this._jsonformsRefresh(name);
  }
};

window.jsonFormsKotlinResolvedTheme = theme => {
  if (theme === 'dark') return 'dark';
  if (theme === 'light') return 'light';
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

window.jsonFormsKotlinDispatchAction = (element, action, params, metadata = {}) => {
  const pending = [];
  let dispatching = true;
  const detail = {
    action,
    params,
    label: metadata.label,
    path: metadata.path,
    element: metadata.element,
    context: metadata.context,
    waitUntil(promise) {
      if (!dispatching) throw new Error('waitUntil must be called while handling jsonforms-action');
      pending.push(Promise.resolve(promise));
    },
  };
  try {
    element.dispatchEvent(new CustomEvent('jsonforms-action', { detail, bubbles: true }));
  } finally {
    dispatching = false;
  }
  return Promise.allSettled(pending).then(results => {
    const firstFailure = results.find(result => result.status === 'rejected');
    if (firstFailure) throw firstFailure.reason;
  });
};

window.jsonFormsKotlinExecuteScript = (host, source, metadata) => {
  const event = {
    action: '',
    label: metadata.label,
    params: metadata.params,
    context: metadata.context,
    element: metadata.element,
    $el: host,
  };
  let run;
  try {
    const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
    run = new AsyncFunction(source);
  } catch (error) {
    const refused = error instanceof EvalError || /unsafe-eval|content security policy/i.test(String(error));
    host.dispatchEvent(new CustomEvent('jsonforms-diagnostic', {
      detail: { code: refused ? 'button.evaluationDisabled' : 'button.scriptCompileFailed',
        message: String(error) }, bubbles: true,
    }));
    throw error;
  }
  return run.call(event);
};
