# Browser-only integration review

This review applies to the pinned extended spec and the current Kotlin Compose renderer. The
`webcomponent` module mounts `ComposeViewport` and passes the common Material extended registry to
`JsonForms`. Its renderer entries are Compose functions; it has no DOM child host or slot bridge.
React and Svelte mount DOM components for these profiles, so adding an npm dependency to the
current webpack bundle alone would not make a Kotlin renderer conformant.

The current custom element creates a viewport `div` inside an open ShadowRoot in `Main.kt`.
`register.js` refreshes attributes on connection and removes its color-scheme listener on
disconnection, but has no Compose viewport disposal callback. There is no per-renderer DOM
insertion point inside the Skia canvas. A browser-only profile therefore needs
the DOM-island proof below before an AG Grid, Monaco or script-engine dependency is useful. The
existing portable fallbacks remain the honest capability status for this webcomponent.

Current checkpoint (2026-11-15): `register.js` passes all 12 Node registration,
reconnection, theme, action and pre-upgrade-property tests. Focused mounted JVM fallback
suites pass five AG Grid cases and 12 Monaco/TemplateLayout cases with no failures or
skips. A standalone Chrome scenario now switches one mounted Web Component through all
three profiles in both development and production bundles: each reports its specific
diagnostic and visible fallback without a form data write. These checks do not supply a
DOM renderer island, viewport disposal or browser engine lifecycle.

The demo bridge also has a serialized-registry limit. Named UI schemas cross the
iframe attribute boundary for Template/Slot lookup, while Kotlin tester functions
cannot be serialized for `REGISTERED` detail selection. The demo now shows a
localized English/Bulgarian notice when such entries are present. JVM tests cover
current data/settings and the named-payload count; the mounted demo Chrome suite
also covers the notice after a rebuilt bundle.

The installed Compose UI version is 1.12.2. The Web Component uses the
`ComposeViewport(viewportContainer = HTMLElement)` overload inside its ShadowRoot; this module
receives no owner handle to dispose on custom-element removal. JetBrains tracks the missing viewport disposal API
as [CMP-9090](https://youtrack.jetbrains.com/issue/CMP-9090), linked from the
[HTML interop discussion](https://youtrack.jetbrains.com/issue/CMP-6858/Be-able-to-use-HTML-elements-from-Compose-Web-HtmlElement).
That discussion includes a report of exhausted WebGL contexts when nested viewports are mounted
repeatedly. Simply removing the viewport `div` in `disconnectedCallback` or making a fresh
viewport on reconnect would not release the Compose/WebGL runtime. Until the host has a proven
teardown path, a browser adapter should not allocate an AG Grid, Monaco model, or template engine
inside this custom element's lifecycle. `WebElementView` may help place an HTML island, but
placement alone does not close the host teardown requirement.

The registration layer refreshes changed attributes when an existing element reconnects,
and defers attribute refreshes while detached. It retains locally edited data when the host has
not changed the `data` or `data-provided` attributes; an explicit attribute update still takes
precedence. A mounted Chrome probe removes and reinserts an edited plain text field, confirms
the same ShadowRoot still has one viewport, and checks that a second edit publishes one change
from the retained value. It deliberately keeps the existing viewport mounted once; this avoids
allocating a second undisposable viewport during a DOM move. It is not teardown.
After selecting an enum popup option, Chrome still exposes the popup layer rather than the
textbox even before detachment; popup dismissal semantics remain a separate open issue.

Button string scripts are a narrower browser feature: they run against the existing custom-element
host and create no DOM island or editor runtime. The webcomponent now requires an explicit
`jsonformsExtended.security.allowScriptEvaluation` config value, executes the async body with the
action event as `this`, and keeps its pending guard through completion or rejection. Its `$el` is
the custom-element host. This does not change the separate DOM-host and teardown gate for
TemplateLayout, AG Grid or Monaco.

| Profile | Reference implementation and required behavior | Current Kotlin result | Decision |
| --- | --- | --- | --- |
| AG Grid array variant | Svelte loads `ag-grid/runtime/AgGridArrayControlRenderer.svelte` only when its tester wins. The spec requires native grid cells wired to the shared cell frame, portable pagination/columns/row detail, explicit native overrides, and safe sorting, filtering and reorder behavior. | Material/Cupertino select the array table for `variant: "ag-grid"`, show a localized English/Bulgarian notice, and report `variant.unsupported`. Both Web Component bundles visibly show this table fallback and diagnostic in Chrome with no data write. The portable table is useful but is not AG Grid. | Keep the portable fallback on all targets. A browser adapter needs a DOM grid host and its own interaction suite. |
| Monaco code control | React lazy-loads `MonacoControlRenderer.impl`; Svelte's profile also uses real Monaco. The spec requires isolated models/workers, JSON draft and schema diagnostics, owned error publication, lifecycle cleanup, production assets and ShadowRoot styles. | No Monaco model/worker bridge is registered. `options.format: "code"` shows `editor.monacoUnavailable` with English/Bulgarian fallback copy and delegates to the family's native form control where one applies; both Web Component bundles show the notice/diagnostic in Chrome without a data write. A converted JSON value is not rewritten on mount. The demo may use a native parse-aware JSON editor under its separate acceptance contract, without claiming Monaco features. | Keep Monaco out of `commonMain`. A browser adapter needs DOM editor hosting and real Monaco lifecycle tests. |
| TemplateLayout | React lazy-loads JSX compilation and has a separate Ractive engine. Svelte chooses JSX or Ractive and has a native template bridge. The spec requires a trust gate, live engine bindings, named delegated children, slot ownership and distinct load/render/unsupported diagnostics. | Portable named `Template`/`Slot` is implemented, but it is a different feature. `TemplateLayout` preserves its authored payload, displays a localized fallback, and reports unknown language, disabled evaluation or unavailable engine in both families. With evaluation explicitly enabled, both Web Component bundles show `template.engineUnavailable` and the visible JSX notice in Chrome without a data write. | Keep script engines out of `commonMain`. A browser adapter requires a DOM template host that can mount delegated Compose children at their original paths; do not reinterpret TemplateLayout as named Template/Slot. |

## Browser adapter gate

1. Prove one DOM island can follow Compose layout bounds, clipping, scrolling, focus and disposal in
   `ComposeViewport`, including a child form renderer at its original data path. If that fails, use
   a separate DOM-based web host rather than claiming a Compose adapter works.
2. Keep the browser registry separate from the shared Material/Cupertino registries. Lazy-load each
   runtime only after its tester wins; preserve the authoring payload and visible diagnostics when
   a chunk or worker fails to load.
3. Run browser interaction tests for the full profile before changing its capability status. For
   AG Grid, test sorting/filtering, row identity, editable-cell focus/caret, portable/native option
   precedence, pagination and row detail. For Monaco, test invalid drafts, model/worker isolation,
   owned errors, language/theme changes, ShadowRoot and teardown. For TemplateLayout, test script
   permission, unknown languages, live bindings, repeated slots, delegated edits and cleanup.
4. Keep native targets on the portable fallback or explicit unsupported path. A web-only adapter
   proves only the browser target, not Kotlin Multiplatform parity.

Formatted-number callbacks follow the same boundary: the spec does not define a serialized
formatter/parser policy. A JavaScript-authored cell may supply runtime functions, but JSON UI
schemas and common Kotlin renderers cannot carry them. A browser host could expose a separate
callback registry without changing numeric storage or the string meaning of `options.format`;
such a registry would need browser-only interaction tests before claiming parity.

Source checks: `../jsonforms-svelte/packages/jsonforms-svelte-flowbite-extended/src/lib/ag-grid/`,
`../jsonforms-react-renderers/packages/jsonforms-react-extended-renderers/src/renderers/MonacoControlRenderer.tsx`,
`../jsonforms-react-renderers/packages/jsonforms-react-extended-renderers/src/renderers/TemplateLayoutRenderer.tsx`,
`../jsonforms-svelte/packages/jsonforms-svelte-extended/src/lib/layouts/TemplateLayoutRenderer.svelte`,
and the sibling spec §§13.5, 18.26 and AG Grid array/pagination sections.
