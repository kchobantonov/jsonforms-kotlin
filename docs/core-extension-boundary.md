# Kotlin core and extended renderer boundary

The pinned spec §1 says the extension model should not require changes to JSON Forms core. This repository owns a Kotlin `core` module; it does not patch the upstream JSON Forms packages. The Kotlin module is the shared parser, data-path, rule and state layer used by both Compose renderer families. Its `commonMain` dependencies are serialization, datetime and the JSON Schema validator, with no Compose UI or browser engine dependency.

| Staged core change | Reason for shared placement | Boundary kept |
| --- | --- | --- |
| `GroupElement.cover`, `header` and `footer`, plus JSON parsing in `UiSchemaJson.kt` | These are part of the serialized Group UI-schema shape. Both families and the demo editor need one lossless model. The new constructor fields have defaults so existing Kotlin call sites remain valid. | Card layout, collapse, semantics and adornment drawing are implemented by Material/Cupertino and Compose modules, not the parser. |
| Group child traversal and mapping in `UiSchemaUtils.kt` | Generic UI-schema operations must visit regional children as well as body elements; otherwise they omit authored controls and rules. | Traversal does not prescribe where a region appears visually or whether it contributes to a Group indicator. |
| `InputAdornments.kt` | Pure JSON grammar, diagnostics and translation resolution are shared by both families. A single parser prevents different accepted wire shapes. | It imports no Compose or platform API. Field slots, icons, tooltips and unsupported-variant notices belong to renderer/Compose modules. |
| `RuleEvaluator.kt` nested schema-scope resolution | Standard schema-based rules need the current data path regardless of which renderer produced the nested Control. Direct JSON Pointers and host function conditions retain their root-data behavior. | The fix applies to generic rule evaluation; Template/Slot remains in `extended-renderers` and only supplies the caller path. |

The extension-specific renderer registry, named Template/Slot resolution and unsupported TemplateLayout fallback remain in `extended-renderers`. Material and Cupertino own their card and field presentations; the Web Component owns the browser host bridge. AG Grid, Monaco and executable TemplateLayout engines have no common-core dependency or claimed KMP implementation.

This audit covers the staged core delta, not every historical `core` API. The Group wire, traversal, adornment grammar and nested-rule changes have focused common tests. The complete core JVM run passed 602 tests with 13 pre-existing skips; the affected mounted Material/Cupertino suites and full workflow JVM step passed after these changes. Android APK packaging passed, while device interaction remains open.
