# Example catalog provenance

The individual Kotlin files in [`examples/upstream`](src/commonMain/kotlin/com/chobantonov/jsonfroms/examples/upstream)
are generated from `@jsonforms/examples`
**3.9.0-alpha.2**, [upstream commit](https://github.com/eclipsesource/jsonforms/tree/2c5fae62a8aace19970ac037d122fa6428cd0436/packages/examples).
The files cover all 79 entries returned by upstream `getExamples()` and store their
serializable data, schema, UI schema, and config as readable literals. Handwritten
Kotlin adapters preserve supported actions, translations, and UI schema registries;
JavaScript callbacks cannot be serialized by the upstream bundle.

The 39 portable spec examples are copied as individual JSON fixture folders in
[`examples/spec`](spec) from
[jsonforms-extended-spec commit `7c354f4f`](https://github.com/kchobantonov/jsonforms-extended-spec/tree/7c354f4f03417c37d91080fcbd169561bce66cd6/examples).
Fixtures that require AG Grid, Monaco, TemplateLayout, or named Template slots
are omitted. The file-control fixture keeps its normal table and omits its AG Grid
category. Those UI integrations have no Compose Multiplatform implementation.
The sync step supplies fallback text for translated `Label` elements that have no
literal `text`, so the shared Kotlin UI schema parser can load them. Gradle generates
the Kotlin catalog under `examples/build/generated/spec-examples` from the checked-in
JSON. Normal Kotlin builds need no sibling checkout or npm package.

Refresh the individual upstream Kotlin files with `node scripts/sync-upstream-examples.mjs`.
Refresh the spec JSON with `node scripts/sync-spec-examples.mjs`; set
`JSONFORMS_SPEC_EXAMPLES_DIR` to use another checkout or unpacked package, and set
`JSONFORMS_SPEC_COMMIT` when the source is not a Git checkout. The spec project's
npm package layout includes JSON fixtures, but Gradle does not consume npm directly.
