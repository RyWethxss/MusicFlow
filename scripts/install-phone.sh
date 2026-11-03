#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
ADB_BIN="${ADB:-adb}"
DEVICE_SERIAL="${1:-}"

if ! command -v "$ADB_BIN" >/dev/null 2>&1; then
  echo "adb was not found. Install Android SDK Platform-Tools or set ADB to its full path." >&2
  exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found. Build it first with ./scripts/build-preview.sh" >&2
  exit 1
fi

if [[ -z "$DEVICE_SERIAL" ]]; then
  mapfile -t DEVICES < <("$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" {print $1}')
  if [[ "${#DEVICES[@]}" -eq 0 ]]; then
    echo "No authorized Android device found. Enable USB debugging, connect the phone, and approve its authorization prompt." >&2
    "$ADB_BIN" devices -l
    exit 1
  elif [[ "${#DEVICES[@]}" -gt 1 ]]; then
    echo "More than one authorized device found. Run: $0 SERIAL" >&2
    printf '  %s\n' "${DEVICES[@]}" >&2
    exit 1
  fi
  DEVICE_SERIAL="${DEVICES[0]}"
fi

"$ADB_BIN" -s "$DEVICE_SERIAL" install -r "$APK_PATH"
printf 'Installed MusicFlow on %s\n' "$DEVICE_SERIAL"
