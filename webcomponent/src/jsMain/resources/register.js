window.jsonFormsKotlinElementFactory = (mount) => class JsonFormsKotlinElement extends HTMLElement {
  static get observedAttributes() {
    return ['data', 'schema', 'uischema', 'readonly'];
  }

  connectedCallback() {
    if (!this._jsonformsMounted) {
      this._jsonformsMounted = true;
      mount(this);
    }
  }

  attributeChangedCallback() {
    if (this._jsonformsRefresh) this._jsonformsRefresh();
  }
};
