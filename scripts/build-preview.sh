#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

VERSION_NAME="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"
if [[ -z "$VERSION_NAME" ]]; then
  echo "Could not read versionName from app/build.gradle.kts" >&2
  exit 1
fi

./gradlew :app:assembleDebug --no-daemon
mkdir -p dist
APK_SOURCE="app/build/outputs/apk/debug/app-debug.apk"
APK_DESTINATION="dist/MusicFlow-v${VERSION_NAME}-preview.apk"
cp "$APK_SOURCE" "$APK_DESTINATION"
sha256sum "$APK_DESTINATION"
printf 'Preview APK ready: %s\n' "$APK_DESTINATION"
