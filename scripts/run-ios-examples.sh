#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_PATH="$ROOT_DIR/examples-compose-ios/ExamplesComposeIos.xcodeproj"
SCHEME="ExamplesComposeIos"
DERIVED_DATA_PATH="$ROOT_DIR/build/xcode/examples-compose-ios"
BUNDLE_ID="com.chobantonov.jsonfroms.examples.compose.ios"

SIMULATOR_NAME="${IOS_SIMULATOR_NAME:-$(xcrun simctl list devices available | awk -F '[()]' '/iPhone/ {gsub(/^[[:space:]]+|[[:space:]]+$/, "", $1); print $1; exit}')}"

if [ -z "$SIMULATOR_NAME" ]; then
  echo "No available iPhone simulator found."
  exit 1
fi

open -a Simulator

if ! xcrun simctl boot "$SIMULATOR_NAME" 2>/dev/null; then
  echo "Using already booted simulator or existing boot session: $SIMULATOR_NAME"
fi

xcrun simctl bootstatus "$SIMULATOR_NAME" -b

xcodebuild \
  -project "$PROJECT_PATH" \
  -scheme "$SCHEME" \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,name=$SIMULATOR_NAME" \
  -derivedDataPath "$DERIVED_DATA_PATH" \
  build

APP_PATH="$DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/ExamplesComposeIos.app"

xcrun simctl install "$SIMULATOR_NAME" "$APP_PATH"
xcrun simctl launch "$SIMULATOR_NAME" "$BUNDLE_ID"
