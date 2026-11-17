# Kotlin renderer capability declaration

This declaration describes the current Compose registries. Registration means a tester and renderer are present; it does not certify every option or target interaction. The [registry inventory](renderer-registry-inventory.json) records every entry and its order, and `node scripts/check-renderer-registry.mjs` detects registry drift. Requirement-level evidence and open behavior are in the [spec ledger](spec-requirement-ledger.json) and [behavior catalog](renderer-behavior-catalog.md).

## Families and targets

| Family | Runtime | Evidence boundary |
| --- | --- | --- |
| Material | Compose Multiplatform on JVM, iOS simulator, JS and Wasm browser; Android main compiles | Broad JVM suites and selected mounted iOS/JS/Wasm cases pass. No blanket device or screen-reader certification. |
| Cupertino | Same Compose targets and shared JSON Forms state/validator | Its own registry and native controls/layouts where present; selected collection and layout renderers call shared Material implementations. Focused mounted target evidence is recorded per requirement. No blanket native-device certification. |
| Extended Material/Cupertino | Added to the matching base registry | Button, color, duration, cron, file, null, Link, ImageView, Separator, splitter, named Template/Slot, Markdown Label and unsupported TemplateLayout entries. Test evidence varies by profile and target. |

The base families register `Control`, `ListWithDetail`, `Label`, `Spacer`, `Group`, `Categorization`, `HorizontalLayout` and `VerticalLayout` presentations. Controls cover scalar text, numeric, boolean, enum, combinators, objects, arrays, tuples and mixed schemas. The portable selections registered in the current families include multiline, switch, radio, autocomplete, slider, password, date/time/date-time, chips, multi-select, stepper, accordion and splitter. A registered presentation can still have option-specific limits; consult the linked behavior catalog and ledger before claiming a particular workflow.

Extended table cells include file, null and compact cron, duration and color editors in both families. Their target and interaction evidence remains scoped in the ledger.

Both families read portable per-element options flat. Global extension settings use `config.jsonformsExtended`; established `restrict`, `disableAdd`, `disableRemove`, `showUnfocusedDescription` and `hideRequiredAsterisk` remain top-level. React, Svelte and vendor option bags are preserved as authored data and ignored by these Compose registries unless a matching adapter explicitly owns them. The demo editor's wire round-trip and the flat-option tester cases cover representative behavior.

## Validator profile

Both families use the shared `OptimumCodeJsonSchemaValidator` through JSON Forms core. It delegates JSON Schema evaluation to the OptimumCode Kotlin Multiplatform validator. The integration honors a declared `$schema`, infers draft-07 for legacy positional `items`, and otherwise selects 2020-12 for a generic `$schema`. It enables format assertions, registers `iso-time`, `iso-date-time`, `cron` and `color`, and adds the temporal comparison keywords described in the [validator notes](../README.md). This is a Kotlin validator profile; it does not claim full Ajv plugin compatibility. The spec's authoring artifacts and imported draft-07 example schemas are checked offline by the separate pinned Ajv gate. General Ajv `$data`, transform, dynamicDefaults and function-bearing schema keywords are not declared as supported Kotlin validator extensions.

## Named browser integrations

| Profile | Material and Cupertino, all declared targets | Observable fallback |
| --- | --- | --- |
| AG Grid `variant: "ag-grid"` | Unsupported as AG Grid | Selects the portable array table, preserves data, displays a localized English/Bulgarian notice and reports `variant.unsupported`. Portable columns, pagination and row detail have separate evidence. |
| Monaco `options.format: "code"` | Unsupported as Monaco | Displays `editor.monacoUnavailable` and delegates to a native applicable control without rewriting the value on mount. |
| JSX/Ractive `TemplateLayout` | Unsupported as a template engine | Preserves the authored payload, displays a localized fallback and reports unknown language, disabled evaluation or unavailable engine. Named Template/Slot is a separate portable feature. |

The [browser integration review](web-only-integration-review.md) records the DOM host, disposal and interaction proof needed before any JS-only adapter can claim one of these integrations. Native Compose equivalents can be declared by their own behavior without claiming compatibility with the named web library. Only English and Bulgarian fallback copy is shipped; other host locales use the documented fallback or a host translator.
