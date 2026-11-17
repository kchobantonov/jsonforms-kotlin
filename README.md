# jsonforms-kotlin

Kotlin Multiplatform port of JSON Forms.

## Current shape

- Root project is a Kotlin Multiplatform multi-project Gradle build.
- AJV-specific coupling is replaced by a `JsonSchemaValidator` abstraction with an `OptimumCodeJsonSchemaValidator` adapter intended for multiplatform JSON Schema validation.

## Modules

- `:core` contains the Kotlin Multiplatform port of the JSON Forms core package.
- `:compose` contains the Compose Multiplatform binding layer on top of `:core`.
- `:material-renderers` contains the first Compose Material renderer set.
- `:cupertino-renderers` contains iOS-style Compose renderers and a Cupertino theme. Its package
  layout mirrors `:material-renderers`; boolean, enum, slider, date, time, and date-time inputs use
  Compose Cupertino widgets, while complex controls preserve the same behavior with Cupertino
  presentation.
- `:extended-renderers` is an optional renderer project. It exports `materialExtendedRenderers`
  and `cupertinoExtendedRenderers`, which add portable controls, layouts, links, images, and basic
  Markdown/Label interpolation to the corresponding base registry.
- `:webcomponent` packages the Material renderer registry as a Kotlin/JS custom element.
- `:examples` contains shared example definitions based on the TypeScript examples package.
- `:examples-compose` contains the shared Compose demo app.
- `:examples-compose-android` contains the thin Android launcher app for the demo.
- `:examples-compose-ios` contains the thin Xcode host app for the demo.

## Example catalog parity

The demo contains all 79 examples registered by `@jsonforms/examples` 3.9.0-alpha.2,
42 portable spec fixtures. The sidebar starts with All examples
and can filter by Spec examples or JSON Forms examples. Hand-written Kotlin examples are retained where they provide Kotlin-specific behavior;
each upstream JSON Forms example has its own Kotlin file under
[`examples/upstream`](examples/src/commonMain/kotlin/com/chobantonov/jsonfroms/examples/upstream), while the spec fixtures live as readable JSON in
[`examples/spec`](examples/spec). Gradle generates their Kotlin catalog during compilation. See [core/UPSTREAM.md](core/UPSTREAM.md)
and [examples/UPSTREAM.md](examples/UPSTREAM.md) for upstream commits and parity notes. Mixed-type schemas and dynamic
`additionalProperties`/`patternProperties` objects have dedicated renderers in both Material and
Cupertino, including type selection and add, rename, and delete operations.

After updating the sibling upstream checkouts, refresh the examples with:

```bash
node scripts/sync-upstream-examples.mjs
node scripts/sync-spec-examples.mjs
node scripts/check-imported-authoring.mjs
```

Set `JSONFORMS_EXAMPLES_BUNDLE` when the upstream bundle is not located at the default sibling
path (`../jsonforms/packages/examples/lib/jsonforms-examples.cjs.js`). Catalog tests compare the
registered Kotlin names to the generated upstream index so omissions and duplicates fail the build.
Set `JSONFORMS_SPEC_EXAMPLES_DIR` to override the default sibling spec fixtures directory.
The spec sync copies portable JSON into `examples/spec`; it does not create a checked-in Kotlin snapshot.
The authoring check validates the imported spec fixtures against the pinned sibling spec's
draft-07 schemas. It requires that checkout's Ajv dependencies and treats the two declared
invalid authoring examples as expected rejections before validating repaired copies.

## Interpolated labels

Extended `Label` renderers accept `options.interpolate: true` with `options.textParams`. A parameter
may read `data`, `item`, `config`, or `context` when `config.jsonformsExtended.dynamicValues.enabled`
is true. Supply JSON host values through the optional `JsonForms(context = ...)` argument; updates
to that argument refresh labels without editing form data. The `item` root is available inside
expanded array, ListWithDetail and table side-panel detail scopes. A translated Label's text sees declared parameters and `locale`,
never the data/context roots directly. Values substituted into Markdown are escaped before parsing.

The current evaluator implements a documented expression subset and reports unsupported expressions
as visible author diagnostics. Canonical CEL, date-time values beyond calendar dates, and target
runtime interaction are still in progress; see the [interpolation profile](docs/interpolation-profile.md)
and [conformance plan](docs/extended-spec-plan.md).

## Temporal bounds

The built-in validator asserts supported JSON Schema `format` values, including
`date`, `time`, `date-time`, `email`, and `uri`, on current and hidden data.
It also validates `iso-time` and `iso-date-time` strings with optional offsets.
Their comparison keywords use the Ajv-format ordering: `iso-time` orders local
clock text without offset conversion, while `iso-date-time` orders calendar
dates first and then compares time at JavaScript Date millisecond precision.
The default Time save format is `HH:mm:ssZ` so newly committed times include
the UTC offset marker required by `format: "time"`. Existing values without an
offset remain editable, but schema format validation reports them as invalid.
Offset-bearing imported Time values are shown as UTC clock time in the picker;
for example, `10:30:00+01:00` displays and saves as `09:30:00Z`. Schema and
picker bound comparisons use the same offset-aware ordering, including fractional
seconds. A time-only value has no date, so this policy does not apply a viewer's
date-specific time-zone rules.

The built-in validator also checks literal and resolvable `$data` values for the
four temporal comparison keywords, including fields outside the current UI.
Literal bounds must be strings; a non-string bound is a schema error even when
the field is absent. Bound strings follow their format comparator: malformed
`date` and ISO bounds can still impose ordering, while `time` and `date-time`
comparisons without a parseable bound impose no limit.
Each comparison keyword requires a string `format` on its schema; an omitted
format produces a schema error at that keyword instead of silently dropping the bound.
The built-in comparison registry supports `date`, `time`, `date-time`,
`iso-time`, and `iso-date-time`;
a comparison keyword on another format reports the missing comparator as a
schema error at that keyword.
Missing `$data` targets add no limit; present non-string targets produce a
schema error. Material and Cupertino controls support literal
`formatMinimum`, `formatExclusiveMinimum`, `formatMaximum`, and
`formatExclusiveMaximum` bounds. A complete typed value outside a bound is saved with a
range validation error. With `restrict` enabled (the default), pickers disable invalid
date and whole-minute choices; `restrict: false` permits those choices. Dynamic `$data`
bound references to string values are resolved by these renderers through
[Ajv-style absolute or relative data pointers](https://ajv.js.org/guide/combining-schemas.html#data-reference).
Bound messages use the control's display format, and an open picker updates its range
feedback when the locale, schema bounds, or referenced data change. A present
non-string referenced bound adds a schema validation error
and disables restricted picker confirmation; a missing reference imposes no limit.
Malformed `$data` references also add a schema error and disable restricted
picker confirmation. A well-formed reference to absent data adds no limit.
Invalid pointer syntax is rejected when the schema compiles, even if its field
is absent. Relative pointers beyond the known depth of a direct property or
array schema are also rejected at compilation. References inside reusable
definitions are checked against their applied data path during validation.
When a restricted picker selection is invalid, its disabled confirmation action also
exposes the localized range explanation through accessibility error semantics.
Typed temporal range errors are attached to the editor's accessibility semantics and
are cleared when the entered value becomes valid.
Other Ajv `$data` keywords and schema compilation diagnostics remain open.

## File size diagnostics

File controls accept `formatMinimum`, `formatExclusiveMinimum`, `formatMaximum`, and
`formatExclusiveMaximum` as byte limits in UI options or form config. A restricted
picker rejects a violating selection with local feedback. An unrestricted picker
commits it and shows local feedback. Set the Control's `propagateErrors: true` to
also publish one owned size error into form validity. A form-wide default can be
set with `config["jsonformsExtended"] = mapOf("propagateErrors" to true)`; an explicit
Control option takes precedence. Publication defaults to false, and only Boolean
`true` enables it. Turning it off retracts the file control's error while preserving
schema and application-supplied errors.

## Numeric input limits


Integer controls accept complete decimal integers within JavaScript's exact numeric range,
`-9007199254740991` through `9007199254740991`, across Kotlin targets. Number controls accept
finite values only when converting the entered decimal to `Double` and back preserves its decimal
value. Incomplete, nonfinite, out-of-range, and precision-losing text stays in the local editor
with an error; it is not written as a string to form data. Combined validity is Pending until
the text is corrected, cleared, replaced by host data, or the control becomes read-only or disabled.
With `restrict` enabled (the preferred default), a complete value outside schema `minimum`,
`maximum`, `exclusiveMinimum`, or `exclusiveMaximum` stays in the editor with a local range error
until corrected. Existing out-of-range data stays visible and can be changed toward the range.
Setting `options.restrict` to false permits the edit while schema validation still reports it.
Increment and decrement actions use `options.step`, then a suitable schema `multipleOf`, then
`1` for integers or `0.1` for numbers. An invalid explicit step shows a configuration error.
Number stepping uses scaled decimal arithmetic and disables an action when its result cannot be
represented exactly within the portable profile; it does not round or change schema validation.
Arbitrary-precision numeric storage is outside this portable profile.

Sliders select zero-based `multipleOf` values within the declared bounds, including exclusive
bounds and integer schemas with fractional multiples. A missing value keeps the schema default
as a provisional thumb position and remains visibly “Not set” until edited; an invalid incoming
value is shown separately from the thumb. Clear removes an ordinary property. A slider range with
no selectable value, unsupported precision, or more than 10,001 selectable positions shows a
diagnostic instead of inventing a value.

For a union offering both `number` and `integer`, changing between those editors keeps the
numeric form value. Selecting the integer editor for a fractional value shows a local integer
error without rounding the stored value or changing document validity. JSON-authored
`options.format` remains a string option; arbitrary formatter/parser functions are not a
portable serialized number format.

## Build

Android-enabled Gradle tasks in this repository require a local Android SDK. Configure one of these before running `build`, `assemble`, or any Android demo task:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

or create a root `local.properties` file:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

You can copy [local.properties.example](/home/krasi/dev/workspaces/kchobantonov/jsonforms-kotlin/local.properties.example) and update the path for your machine.

Build everything from the repository root:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew build
```

If you only want assembled artifacts:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew assemble
```

Build individual modules:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :core:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :compose:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :material-renderers:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :cupertino-renderers:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :extended-renderers:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :webcomponent:jsBrowserProductionWebpack
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose-android:assembleDebug
```

If Kotlin/JS reports that the yarn lock is outdated, update it with:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew kotlinUpgradeYarnLock
```

## Run Demo

The Kotlin/JS demo is built with `:examples-compose:jsBrowserProductionWebpack`. Pushes to
`master` deploy the demo to GitHub Pages; the web component sample is available under
`/webcomponent/` on the same site.
The root web page opens on a welcome screen; **Open Demo** selects the first example. Direct links
use percent-encoded example names, for example `#i18n` (`#example%20name` illustrates a name
with a space); older
`#/examples/i18n` links remain readable. View state lives in the URL, for example
`#i18n?form-only=true&active-tab=schema`. Supported parameters are `read-only`, `form-only`,
`active-tab` (`demo`, `schema`, `uiSchema`, `uiSchemas`, `internationalization`, `config`, `data`),
`use-webcomponent`, and `drawer`. The demo reads them from the normal query or the hash query,
with hash values taking precedence. Changes to the view write the hash query and preserve unrelated
parameters. Browser Back and Forward restore the view while retaining the selected example's
working data. Only literal `true` enables a Boolean parameter.
On the Kotlin/JS demo, `use-webcomponent=true` opens the browser-only Web Component in an
embedded frame. It also works with `form-only=true`; **Show full UI** returns to the full
Web Component view without resetting form data. Other targets keep the Compose renderer.
The example header links to the exact source at the catalog's pinned commit. Adapted spec
fixtures show an English or Bulgarian capability notice describing omitted browser integrations.
The [renderer capability declaration](docs/renderer-capabilities.md) lists registered Kotlin profiles,
the shared validator, target evidence limits and data-preserving browser-integration fallbacks.

Desktop, Android, and iOS use the same in-app example picker, tabs, form-only action, and settings
controls as session navigation. They have no browser URL or hash, so their view state remains in the
running app. The form-only view hides the example picker, tabs, title, data editor, and settings;
its small **Show full UI** action restores the previous workspace.

To run the mounted JS and Wasm URL/back-forward tests after building the development bundles:

```bash
./gradlew :examples-compose:jsBrowserDevelopmentWebpack -Pjsonforms.skipWebcomponentResources=true
node --test scripts/test-demo-browser-url.mjs
./gradlew :examples-compose:wasmJsBrowserDevelopmentWebpack -Pjsonforms.skipWebcomponentResources=true
DEMO_BROWSER_TARGET=wasmJs node --test scripts/test-demo-browser-url.mjs
```

The browser demo's top toolbar can switch the active form between Compose and the web component.
The web component is an optional integration for plain HTML and other web apps. Its separate
sample page shows attribute input, categories, conditional rules, and change/action events.
It renders the Compose canvas inside a custom element. Run the standalone sample locally with:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :webcomponent:jsBrowserRun
```

To embed the web component, copy the entire `:webcomponent` production bundle so its Skiko assets
stay beside `jsonforms-kotlin-webcomponent.js`, then load `register.js` followed by
`jsonforms-kotlin-webcomponent.js` and use `<jsonforms-kotlin-compose>`
with JSON strings in `data`, `schema`, and optional `uischema` and `config` attributes.
The optional `uischemas` attribute accepts named definitions as a JSON array of
`{"uischema":{"type":"VerticalLayout","name":"details","elements":[]}}` entries;
ranked tester functions cannot cross the attribute boundary. A direct JavaScript host can set
`element.uiSchemaRegistry` to an array of `{tester, uischema}` entries. The tester receives
`(schema, schemaPath, path)` and returns a numeric rank (`-1` means not applicable); `schema`
is a plain JavaScript object. Assigning the property refreshes the form, including when the
same array instance is reassigned after editing its entries. Assignment before the custom
element definition loads is also preserved when the element upgrades. For example:

```js
const form = document.querySelector('jsonforms-kotlin-compose');
form.uiSchemaRegistry = [{
  tester: (schema, schemaPath, path) =>
    schema?.type === 'object' && path === 'address' ? 10 : -1,
  uischema: { type: 'VerticalLayout', elements: [
    { type: 'Control', scope: '#/properties/street' }
  ] }
}];
```

If a tester throws, the form uses generated detail and emits a bubbling `jsonforms-diagnostic`
event with code `uischema.testerFailed`, the data path, and the error message.
JavaScript functions cannot pass through the demo's iframe attribute bridge. `translations` accepts
flat string catalogs by locale, such as `{"en":{"title":"Title"},"bg":{"title":"Заглавие"}}`.
The optional `context` attribute accepts a JSON object, including `external` and host-defined
fields. Its values are available in action events; live form fields override matching host keys.
The optional `additional-errors` attribute accepts a JSON array of host validation errors with
`instancePath`, `message`, and optional `schemaPath`, `keyword`, and string `params` fields.
Set `lang` to `en` or `bg`, `theme` to `light` or `dark`, and `dir` to `ltr` or `rtl`;
unsupported language values fall back to English. Attribute changes refresh the mounted form.
Set `validation-mode` to `validate-and-show` (the default), `validate-and-hide`, or
`no-validation`. Use `data-provided="false"` to distinguish missing form data from an explicit
JSON `null` value; omitting the attribute means data is provided.
Changes emit a bubbling
`jsonforms-change` event whose `detail.data` and `detail.dataProvided` are the
updated value and presence state. Button actions emit `jsonforms-action` with `detail.action`, `detail.params`,
`detail.label`, `detail.path`, `detail.element`, and a context snapshot containing data, schema,
root UI schema, locale, read-only state, validation errors, host additional errors, JSON config and
the supplied host context. For JSON-authored Buttons, `detail.element` retains
the authored rule and extension fields.
An action listener can call `event.detail.waitUntil(promise)` during dispatch; the button remains
pending until all registered promises settle. Rejected promises propagate to the browser's error
handling and clear the pending state.
Compose hosts can register `ExtendedActionEventHandler` under
`ExtendedActionEventHandlerConfigKey` to receive the action, translated label, normalized params,
triggering Button element/path, and a form-context snapshot. This callback takes precedence over
the older `ExtendedActionHandler(action, params)` callback when both are configured.
String Button scripts require a `config` JSON attribute containing
`{"jsonformsExtended":{"security":{"allowScriptEvaluation":true}}}`.
The web component runs them as async function bodies with the action event as `this` (`$el` is the
custom-element host). Grant this only to trusted UI schemas. A restrictive content-security policy
remains authoritative; blocked compilation emits `jsonforms-diagnostic`.
The JSON wire format supports string scripts only. Kotlin does not expose a function-valued Button
script callback; use the action handler for host-defined behavior. Button and Control visibility
only changes presentation. The host service must authorize every operation independently of the
rendered UI and reject unauthorized requests.
The element fills the size assigned by its host page.

To run the mounted action test, build the development bundle and then run the Chrome test
(`CHROME_BIN` may point to a different Chrome executable):

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :webcomponent:jsBrowserDevelopmentWebpack
node --test scripts/test-webcomponent-browser.mjs
```

To exercise the shipped bundle instead, run:

```bash
./gradlew :webcomponent:jsBrowserProductionWebpack
WEBCOMPONENT_BUNDLE_MODE=production node scripts/test-webcomponent-browser.mjs
```

The demo's Settings panel has an **Interface Style** selector:

- **System** uses Cupertino on iOS and Material on other platforms.
- **Material** forces Material on every platform, including iOS.
- **iOS** forces Cupertino styling on every platform, including Android, desktop, and web.

The selected style and light/dark preference apply to both the demo shell and the JSON Forms
renderer registry. Material and iOS styles each have independently selectable light and dark
color themes: Blue, Indigo, Purple, Green, Orange, and Pink. All choices are persisted.

Desktop:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose:run
```

Android emulator:

1. Start an Android emulator from Android Studio or Device Manager.
2. Make sure the Android SDK is configured via `local.properties`, `ANDROID_HOME`, or `ANDROID_SDK_ROOT`.
3. Run:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose-android:runDebug
```

Web with Kotlin/JS:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose:jsBrowserDevelopmentRun
```

If the development page fails at startup inside Kotlin/JS library code after switching
between production and development builds, stop the dev server and rebuild its shared
webpack package with `./gradlew clean :examples-compose:jsBrowserDevelopmentWebpack`.
Then restart `jsBrowserDevelopmentRun` and reload the page.

The JS browser page is served from:
- [index.html](/Users/kchobantonov/dev/workspaces/kchobantonov/jsonforms-kotlin/examples-compose/src/jsMain/resources/index.html)

Web with Kotlin/Wasm:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose:wasmJsBrowserDevelopmentRun
```

The Wasm browser page is served from:
- [index.html](/Users/kchobantonov/dev/workspaces/kchobantonov/jsonforms-kotlin/examples-compose/src/wasmJsMain/resources/index.html)

## Responsive horizontal layouts

`HorizontalLayout` preserves the UI schema's horizontal direction on every screen size. Its children receive equal width within the row.

To let a particular horizontal layout stack its children on screens narrower than `600dp`, opt in on that layout:

```kotlin
HorizontalLayout(
    options = mapOf("stackOnSmallScreen" to JsonPrimitive(true)),
    elements = listOf(/* ... */),
)
```

Layouts without this option remain horizontal, which is useful for compact controls such as two checkboxes displayed side by side.

iOS:

- The shared iOS entry point is exposed as `MainViewController()` in [MainViewController.kt](/Users/kchobantonov/dev/workspaces/kchobantonov/jsonforms-kotlin/examples-compose/src/iosMain/kotlin/com/chobantonov/jsonfroms/examples/compose/ios/MainViewController.kt).
- The thin host app lives in [examples-compose-ios](/Users/kchobantonov/dev/workspaces/kchobantonov/jsonforms-kotlin/examples-compose-ios).
- Start it in the simulator with:

```bash
./scripts/run-ios-examples.sh
```

- If you want a specific simulator, set `IOS_SIMULATOR_NAME`, for example:

```bash
IOS_SIMULATOR_NAME="iPhone 16" ./scripts/run-ios-examples.sh
```

## Notes

- The repository uses a local Gradle home in examples above to keep build state inside the workspace.
- Some full multiplatform builds can be memory- and disk-intensive, especially native framework linking and Android dexing.
