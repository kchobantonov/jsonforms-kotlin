# Extended specification implementation plan

Status: implementation in progress. Prepared 2026-11-10; updated after initial Template/Slot and owner-validity slices.
Execution queue: [TODO.md](../TODO.md).

## Scope and authority

Implement the portable extended specification in both Material and Cupertino renderer families on Android, desktop JVM, iOS arm64/device and simulator, JS browser and Wasm browser. Preserve the existing core, Compose binding, optional extended renderers and example modules. A successful build is not conformance evidence.

Ship English and Bulgarian locale defaults only. Resolve regional Bulgarian tags to Bulgarian; use English for other unsupported locale tags. Keep host translator overrides so applications can supply their own strings without adding a third shipped locale.

Authority: extended spec and renderer/demo acceptance guide, then renderer-selection guide, schemas and conformance fixtures. React Antd/shadcn and Svelte implementations/tests supply regression scenarios, not permission to copy their gaps. Read normative amendments after the appendix heading; distinguish them from explicitly experimental proposals.

Reference checkouts (clean at planning inspection):

| Repository | Revision |
| --- | --- |
| jsonforms-extended-spec | 7c354f4f03417c37d91080fcbd169561bce66cd6 |
| jsonforms-react-renderers | 1dac6d36057906bb72ce735af6a953fd50d76961 |
| jsonforms-svelte | b0fb61ed002bf377108089bcc7f1251d6884ffc6 |
| jsonforms-kotlin | ca3ce86580b92290b3ae11aacc3d35e953e4190c |

The current source inventory and example catalog use `@chobantonov/jsonforms-extended-spec` version `0.2.0`, pinned in the root `package-lock.json`. That release corresponds to spec tag `v0.2.0` and commit `e0b23861ca0c47d17309202c5dbb2111364b3286`. Run `npm ci` and `npm run spec:check` to verify the package, requirement inventory, conformance inputs, and checked-in example snapshot. The table above records the original planning inspection revisions.

Primary reading under `node_modules/@chobantonov/jsonforms-extended-spec`: docs/spec.md, docs/renderer-and-demo.md, docs/renderer-selection.md, docs/implementation-guide.md, docs/implementation-pitfalls.md, docs/conditional-fields.md, docs/draft-07-visual-support.md; schemas; conformance/{authoring,behavior,config-consumption,expected-invalid}.json; examples/catalog.json.
Reference lessons: ../jsonforms-react-renderers/docs/TODO.md and relevant changesets/tests; ../jsonforms-svelte/docs/{spec-upgrade-plan,renderer-porting-checklist,spec-section-evidence,renderer-parity-audit,renderer-owned-validation,rule-validation-isolation,dynamic-child-readers,localization-source-audit}.md and feature-specific native reviews.

Planning reviewed the spec section inventory, i18n contract, demo layout/settings, implementation guidance and reference backlogs, plus Kotlin build files, registries, test inventory and selected runtime contracts. This is not an exhaustive source audit. Phase 0 explicitly finishes the atomic requirement audit before feature completion claims.

## Existing baseline

The project already has core state/validation/path/translation APIs, Material and Cupertino renderer registries, extended button/color/duration/file/null/split/slot entries, shared examples and common tests. Both Material and Cupertino currently declare empty cell registries; 11 Cupertino renderer entries delegate directly to Material implementations. Inventory cell dispatch and delegated UI roles before claiming compact-cell or native-family parity. DynamicValueContext handles preservation of empty dynamic-property values; its name is not proof of §11 overlay support.

The README reports 39 portable spec fixtures; compare actual catalog IDs with the pinned spec, not this historical count. Existing helper tests and catalog presence are useful but do not prove real renderer interaction.

Prior session verification: Android assembleDebug, desktop jvmJar, iOS simulator Kotlin compilation, JS and Wasm production webpack passed. Xcode simulator app built with ARCHS=arm64 ONLY_ACTIVE_ARCH=YES; default build requested unsupported x86_64. No full test suite was executed. Carry the Xcode architecture issue into the platform gate.

## Coverage system: prevent omissions

Create a versioned machine-readable requirement ledger, human-readable capability matrix and verification log. Every MUST/SHOULD, schema/config/local-option property, renderer/cell role, behavior vector and example gets a stable row or explicit mapping to a row. Include all spec sections 1–30 and normative amendments. Suggestions/proposals get explicit classification, not silent deletion.

Each row records: ID; exact pinned source/section; normative strength; portability/profile; behavior; defaults/precedence; Kotlin consumer; Material and Cupertino status; target support; reference test/scenario; fixture; unit/renderer/UI tests; last executed command/result/revision; limitation; next action. States: not verified, unsupported, partial, supported. Track applicability separately so an exclusion never counts as a pass.

Generate checks for unmapped schema fields, config-consumption IDs, authoring/behavior IDs and example IDs. An upstream refresh must show additions/removals and require classification. Never auto-mark imported vectors as executed. Tests must invoke the Kotlin adapter, assert results, and report unsupported cases separately.

Inventory all registry entries and delegated roles, including cells. For each option prove selection, consumption and observable behavior; a model field or parser accepting it is insufficient. Use current schema properties as well as conformance inventories because inventories can lag newer amendments.

## Architecture

- core: typed JSON identity, exact paths, schema/rule adapters, configuration precedence, non-UI detail resolution, diagnostics and translation contracts.
- compose: state/lifecycle integration, registry dispatch, owner-scoped draft/errors/pending operations, confirmation and shared interaction services.
- material-renderers / cupertino-renderers: native UI composition, semantics, focus, family styling and renderer/cell adapters.
- extended-renderers: optional portable advanced controls; isolate heavyweight evaluator/editor dependencies from base consumers where practical.
- examples / examples-compose: pinned fixture import and host/demo state. Runtime consumers must not depend on demo fixtures.
- Platform adapters: file selection/read lifecycle, URL opening, preferences, locale formatting and platform-specific UI/testing entry points.

Use stable occurrence identity separate from labels, row indices and authored UI objects. Cancel stale async work when data, path, schema, permissions or owning occurrence changes. Preserve unknown vendor properties and JSON round trips. Keep selection/navigation, expansion, focus and drafts out of authored schema and committed form data.

## Phases and exit gates

### P0 — Inventory and harness

Finish atomic ledger and source inventory; inspect relevant React and Svelte implementation/tests for each row. Record gaps without assuming old reference TODOs are current failures. Pin fixture provenance and reproducible import; classify every excluded example. Discover actual Gradle tasks and test-runner support. Establish one baseline full verification checkpoint, recording existing failures separately.

Exit: all requirement sources mapped, no unclassified examples/options, test-task manifest and capability statuses published.

### P1 — Dependency and native-component feasibility

CEL: first assess [OpenSavvy CEL for Kotlin](https://gitlab.com/opensavvy/cel-kotlin), explicitly advertised as experimental. Verify published artifacts, license, maintenance, parser/checker/runtime completeness, registered functions, JSON type mapping, missing values, evaluation budgets and actual Android/JVM/iOS/JS/Wasm variants. Run a small shared conformance corpus on every target before adoption. JVM-only [cel-java](https://github.com/cel-expr/cel-java) may serve as a comparison oracle, not a commonMain dependency.

Prefer one KMP CEL implementation. If it fails the gate, record exact missing semantics and choose a scoped upstream contribution/portable implementation or explicitly partial capability. Do not substitute arbitrary expression syntax, host eval or a regex evaluator while claiming CEL compatibility. Separate canonical interpolation from experimental overlays.

Locale formatting: evaluate [kotlinx-locale](https://github.com/carcaradotdev/kotlinx-locale) against number/currency/date requirements and target variants. Candidate status is not an endorsement or verified compatibility. Fallback is one common interface with Android ICU, JVM formatter/ICU4J, Apple Foundation and JS/Wasm Intl implementations, as illustrated by [Kotlin regional-format guidance](https://kotlinlang.org/docs/multiplatform/compose-regional-format.html). Test locale changes, currency digits, grouping, negative values and calendar dates across targets. kotlinx-datetime alone does not establish locale formatting.

The spec §9 canonical message syntax is CEL, not ICU MessageFormat. ICU can back formatting; ICU message patterns require a separately named optional extension and must not reinterpret canonical messages.

For installed Compose Material3 and io.github.schott12521:cupertino versions, inspect actual source/API and target availability. Create a component decision table per control/cell/demo element: component, version, native behavior, required wrapper, gap and exception. Prefer official native components, then documented compositions, then minimal custom behavior. Cupertino-looking Material widgets do not establish Cupertino fidelity. Review text/select/search/tokens/radio/cards/switch/slider/password/OTP/date/time/dialogs/tabs/disclosure/tree/splitter/tooltips and scrollbars. Do not assume a native splitter or picker exists.

Exit: small dependency probes pass or documented fallback decision; native component inventory approved through evidence. No unbounded evaluator port hidden inside a renderer task.

### P2 — Shared correctness and host validity

Implement/reconcile missing/null/false/zero/empty identity; structural JSON choice equality; literal keys and JSON Pointer ownership; root and referenced schema handling; canonical namespacing and local-over-global precedence including explicit false and collection replacement.

Separate document schema errors, active-branch local feedback, host additional errors, renderer draft errors, transient operation feedback and author diagnostics. Add owner-scoped replace/clear/move/dispose, combined validity/pending status, stale async rejection, nonmutating rule validation, touch filtering, readable direct container errors and deduplicated descendant summaries.

Centralize confirmation and pending-edit flush/cancel. Recheck permissions when callbacks complete. Bound reference traversal and cache compilation without conflating recursion at distinct data paths.

Exit: real host/registry tests cover these contracts, not only helper functions.

### P3 — Internationalization and presentation foundation

Provide shared enumerated renderer catalogs for English and Bulgarian, base-language fallback and runtime locale switching. Lookup exact literal catalog key before nested keys. Pass locale-bundle strings as translator defaults; form catalog wins. Translate all tooltips, accessible names, empty states, errors, confirmations, indexes, summaries, choices and demo chrome.

Test locale with no custom translator, partial catalogs and missing keys. Implement authored text precedence and §9 pipeline: effective parameters, lookup, expressions, substitution escaping, safe markup. Separate parameter namespaces from catalog expression scope; distinguish absent data from author error. Register number/currency/date/translate functions; date-only formatting must not shift calendar day. Test literal braces, constructor keys and injection-shaped substitutions.

Define native field/cell/container anatomy, theme tokens, RTL, semantics and keyboard/touch feedback. Preserve required semantics when hiding asterisks. Keep ordinary field errors inline and compact-cell errors locally discoverable.

### P4 — Scalar controls, choices and cells

Deliver vertical slices for text/multiline/masks/password/OTP, boolean, numeric/formatted/slider, temporal and choice presentations. Include supported restrictions, clear semantics, missing/invalid/imported data, immutable external replacement and IME/draft behavior.

Choice cases include structural object/array values, radios/cards/search/multi-select/tokens and out-of-domain values. Numeric editor intent must survive whole-number values without rounding. Temporal storage and display differ; test views, offsets, date-only values and incomplete input. Implement Material and Cupertino cells with native family styling and one frame.

### P5 — Containers, composition and layouts

Cover object/dynamic properties, pattern intersection, propertyNames, additionalProperties, tuple prefixes/tails, arrays/table/list/details, pagination and all detail modes; mixed tree/navigation; allOf/anyOf/oneOf/conditional fields/references; branch cards; recursion/depth; false schemas, contains and invalid existing data.

Central detail resolver must distinguish GENERATE/REGISTERED/inline and carry context through delegation. Use stable row/occurrence identity through reorder/delete/filter/rename. Keep direct and descendant errors at their correct boundary. Support visible-child sizing, spans/gaps/wrap, splitters, Spacer, Group and categorization variants. Authored positions and inactive data survive conditional changes.

### P6 — Portable extended features and diagnostics

Cover actions and registered async commands, color/duration/file/null/cron, image/link/separator/markup, named Template and Slot reuse. Add cycles/missing-name/duplicate-name diagnostics. Keep script gates closed by default and document unsupported host scripting.

Files require platform pickers on every supported host, cancellation and stale-read guards. Restricted rejection or failed replacement preserves committed validity with local feedback; successfully committed unrestricted size-invalid data gets local feedback and an owned error only when `propagateErrors` is enabled per element or namespaced config. Pending operations gate commands. Do not infer original file bytes from imported encodings.

### P7 — Demo acceptance

Build against renderer-and-demo.md §§6–8 and §30, with both family themes:
- Fixed top header: left menu/brand/home; right form-only, webcomponent where available, repository, settings in that order.
- Desktop sidebar below header shifts content and scrolls independently; searchable wrapped labels, active item and empty result. Narrow view uses dismissible drawer.
- Example title above native contained tabs: Demo/Data, Schema, UI Schema, UI Schemas, Internationalization, Config, separate Data when needed. Demo tab carries validation count; example actions align with JSON Forms heading.
- Desktop form/data starts near 75/25 with accessible resizable splitter and usable minimums; stack on narrow windows and respect RTL.
- Every JSON-aware editor has Reload/Apply; invalid drafts never replace active models. Reload restores original example text but only Apply changes the form. A native parse-aware JSON editor is acceptable without Monaco; do not advertise Monaco-specific features.
- Settings in end-side overlay with close/focus/backdrop/scroll: system/light/dark, direction, English/Bulgarian locale, validation, layout, restrict, readonly and all specified common toggles; native provider theme settings.
- Persist mode/locale/layout/family preferences through versioned platform storage. Session toggles remain session-only by default. Recover from corrupt/unavailable storage.
- Form-only preserves data/settings; home includes brand, title, tagline and open-first-example.
- Browser URL/hash/query precedence, encoding and back/forward behavior follow spec. Native hosts get equivalent state/navigation with documented platform applicability.
- Example source links/provenance, capability notices and independent application/form locale/theme actions, especially Job application.

### P8 — Optional profiles and final verification

Experimental §11 dynamic overlays stay separately labelled; add only after evaluator feasibility and explicit profile acceptance. Cover recomputation from static state, per-occurrence child/parent readers before tester selection, trust/readiness, fallback, bounded snapshots, stable identity, disposal, IME and focus. Preserve existing static fast paths.

Finish platform interaction, accessibility, performance and full-suite checkpoint. Publish per-family/per-target evidence and remaining limitations. Never mark skipped UI tests or unsupported profiles as passed.

## Web-specific scope decisions

Defer JSX/Ractive TemplateLayout, Monaco and AG Grid runtime integrations unless a proven all-platform implementation satisfies their contract; embedding a web view alone is not parity. Keep explicit unsupported diagnostics and preserve authoring payloads. The `ag-grid` array variant currently dispatches to the portable table with a visible notice and `variant.unsupported` diagnostic; this fallback is not AG Grid conformance. A future JS-only adapter may be registered separately for the webcomponent, with its own browser interaction tests, while the shared Material/Cupertino registries retain portable behavior. Portable named Template/Slot, JSON draft editing, table/cell/detail behavior, sorting/filtering summaries and layout reuse remain in scope. Keep the existing JS webcomponent working but do not promise it on native hosts. Graphical authoring and remote-choice proposals are outside initial runtime conformance.

The sibling Svelte implementation keeps AG Grid behind a lazy browser runtime (`ag-grid/lazy-loader.ts`) and its webcomponent chunk configuration; the React demo uses `@monaco-editor/react`'s loader. These are web integrations rather than common Kotlin renderer dependencies. Revisit a JS-only adapter only after the portable table/detail and demo editor contracts have their own interaction evidence.

The [browser-only integration review](web-only-integration-review.md) records the current ComposeViewport boundary, each profile's actual fallback/diagnostic, and a DOM-host proof gate before any JS-only adapter is advertised. TemplateLayout now has an explicit unsupported renderer in both shared families; Monaco code mode shows an explicit notice and delegates to the existing native form control where applicable.

The spec's URL-template component encoding applies when a template engine substitutes into URL attributes or an experimental `$dynamic.template` descriptor. Literal `Link.href` and `ImageView.src` remain literal values and still pass the final URL policy. Keep the encoding requirement with the relevant template adapter rather than enabling interpolation on those fields.

## Regression checklist from React and Svelte

For each feature inspect current reference tests and port outcomes, not framework mechanics:
- Exact keys including dots/slashes/brackets/empty/NUL; lossless key snapshots; overlapping patterns.
- Composition preserved during type choice; enclosing fields once; branch-local errors distinct from document validity; sole branch without pointless selector.
- Invalid imported values visible/removable; selection identity independent of labels; repeated selection after rejected async confirmation.
- Pending edits flushed before navigation/Apply; cancelled or stale callbacks cannot mutate removed/rebound/readonly fields.
- Owner errors survive independent host errors and move/clear with the exact occurrence; touch filtering does not hide direct errors accidentally.
- GENERATE bypasses registry even through object delegation; row detail context uses actual item/root scope.
- Compact cells do not duplicate labels/frames; container errors do not replace local feedback; required semantics survive hidden marker.
- Native popups bounded and scrollable; focus, escape, keyboard, touch, translated long labels, RTL and dialog geometry tested.
- File failure versus committed invalid data; asynchronous read disposal.
- Whole data replacement and locale/theme changes preserve appropriate draft/selection state.
- No English-only fallback disguised by a translator that answers every key; app and form themes/locales remain independent.

## Verification policy

No full build after each edit. Batch coherent changes and choose verification from impact:
1. Pure shared change: focused JVM common tests in changed module plus affected consumers. Example candidate: ./gradlew :core:jvmTest --tests '<test class>'; confirm task/filter support in P0.
2. Renderer change: focused real-registry Compose interaction tests for both families, plus targeted visual/geometry checks when placement changes. A helper-only test cannot close UI acceptance.
3. Platform bridge or dependency change: compile affected targets and execute relevant target tests; CEL/formatting probes must run across all targets.
4. Demo change: targeted editor/state/navigation tests and one affected-host UI smoke; production webpack only for bundling/resource/webcomponent concerns.
5. Shared integration milestone: impacted module suites and target smoke builds.
6. Full gate: baseline once, final acceptance/release, toolchain/source-set/public dependency changes with broad risk, or unexplained cross-target regression. Run all available module tests, native/browser/device UI suites and production builds; record environments and unavailable runners as outstanding.

Maintain the [test-task manifest](test-task-manifest.md): task, module, platform, unit/UI/artifact purpose, prerequisites and trigger. Gradle build/allTests alone does not guarantee device UI tests or Swift app linking. Include Android device tests, iOS arm64 simulator tests and Xcode host linking, JVM desktop, JS and Wasm browser tests plus production bundles. Never clean by default; reuse Gradle caches. Record command, revision, exit code and evidence; rerun only after relevant edits/failure. Performance checks target active-branch validation caching, typing latency, static dispatch, large collections and native resource lifetimes.

## Definition of done for a feature

A ledger row closes only when implementation, option precedence, both family adapters, applicable targets, i18n, accessibility, regression tests and demo evidence are present. Exercise mount/edit/clear, invalid/missing data, external replacement, disabled/readonly/restrict, locale change, keyboard/touch and narrow layout. Record any unsupported combination explicitly. Update TODO and verification evidence in the same change. Reference bugs do not waive the spec.
