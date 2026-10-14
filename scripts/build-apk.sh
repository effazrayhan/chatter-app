#!/usr/bin/env bash
set -euo pipefail

PASS_NAME="${1:-}"
if [[ -z "$PASS_NAME" ]]; then
  echo "Usage: $0 <pass-name>" >&2
  exit 2
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
./gradlew -PpulsePass="$PASS_NAME" assembleDebug

SOURCE_APK="app/build/outputs/apk/debug/app-debug.apk"
OUTPUT_DIR="$ROOT_DIR/dist"
OUTPUT_APK="$OUTPUT_DIR/pulse-${PASS_NAME}-debug.apk"
if [[ ! -f "$SOURCE_APK" ]]; then
  echo "APK was not produced: $SOURCE_APK" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
cp "$SOURCE_APK" "$OUTPUT_APK"
SIZE="$(stat -c '%s' "$OUTPUT_APK")"
SHA256="$(sha256sum "$OUTPUT_APK" | awk '{print $1}')"
echo "APK: $OUTPUT_APK"
echo "Size: ${SIZE} bytes"
echo "SHA-256: $SHA256"

if command -v aapt2 >/dev/null 2>&1; then
  aapt2 dump badging "$OUTPUT_APK" | grep -m1 "package:"
elif command -v apkanalyzer >/dev/null 2>&1; then
  apkanalyzer manifest application-id "$OUTPUT_APK"
  apkanalyzer manifest version-code "$OUTPUT_APK"
  apkanalyzer manifest version-name "$OUTPUT_APK"
else
  SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
  AAPT2_PATH="$(find "$SDK_ROOT/build-tools" -type f -name aapt2 -perm -u+x 2>/dev/null | sort -V | tail -n1)"
  if [[ -n "$AAPT2_PATH" ]]; then
    BADGING_FILE="$(mktemp)"
    "$AAPT2_PATH" dump badging "$OUTPUT_APK" > "$BADGING_FILE"
    grep -m1 "package:" "$BADGING_FILE"
    rm -f "$BADGING_FILE"
  else
    echo "Version metadata: use aapt2 or apkanalyzer to inspect the APK"
  fi
fi
