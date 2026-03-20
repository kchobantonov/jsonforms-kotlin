# jsonforms-kotlin

Kotlin Multiplatform port of JSON Forms.

## Current shape

- Root project is a Kotlin Multiplatform multi-project Gradle build.
- AJV-specific coupling is replaced by a `JsonSchemaValidator` abstraction with an `OptimumCodeJsonSchemaValidator` adapter intended for multiplatform JSON Schema validation.

## Modules

- `:core` contains the Kotlin Multiplatform port of the JSON Forms core package.
- `:compose` contains the Compose Multiplatform binding layer on top of `:core`.
- `:material-renderers` contains the first Compose Material renderer set.
- `:examples` contains shared example definitions based on the TypeScript examples package.
- `:examples-compose` contains the shared Compose demo app.
- `:examples-compose-android` contains the thin Android launcher app for the demo.
- `:examples-compose-ios` contains the thin Xcode host app for the demo.

## Build

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
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose:build
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose-android:assembleDebug
```

If Kotlin/JS reports that the yarn lock is outdated, update it with:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew kotlinUpgradeYarnLock
```

## Run Demo

Desktop:

```bash
GRADLE_USER_HOME=$PWD/.gradle-user-home ./gradlew :examples-compose:run
```

Android emulator:

1. Start an Android emulator from Android Studio or Device Manager.
2. Make sure `adb` is available under the Android SDK pointed to by `local.properties`.
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
