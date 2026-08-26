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
- `:examples` contains shared example definitions based on the TypeScript examples package.
- `:examples-compose` contains the shared Compose demo app.
- `:examples-compose-android` contains the thin Android launcher app for the demo.
- `:examples-compose-ios` contains the thin Xcode host app for the demo.

## Example catalog parity

The demo contains all 78 examples registered by the sibling JSON Forms TypeScript examples
package. Hand-written Kotlin examples are retained where they provide Kotlin-specific behavior;
the remaining examples are generated into a portable snapshot. Mixed-type schemas and dynamic
`additionalProperties`/`patternProperties` objects have dedicated renderers in both Material and
Cupertino, including type selection and add, rename, and delete operations.

After updating the sibling JSON Forms checkout, refresh the portable examples with:

```bash
node scripts/sync-upstream-examples.mjs
```

Set `JSONFORMS_EXAMPLES_BUNDLE` when the upstream bundle is not located at the default sibling
path (`../jsonforms/packages/examples/lib/jsonforms-examples.cjs.js`). Catalog tests compare the
Kotlin names to the upstream snapshot so omissions and duplicates fail the build.

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
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose-android:assembleDebug
```

If Kotlin/JS reports that the yarn lock is outdated, update it with:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew kotlinUpgradeYarnLock
```

## Run Demo

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
