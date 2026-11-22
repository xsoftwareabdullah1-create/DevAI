#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK_DIR="$ROOT/app/src/main/cpp/third_party/litertlm"
ZIP_PATH="${RUNNER_TEMP:-/tmp}/litert_lm_c_api-1.0.0.zip"
URL="https://github.com/google-ai-edge/LiteRT-LM/releases/download/v0.19.1/litert_lm_c_api-1.0.0.zip"
SHA256="1d430d80eaf536f696a4922270ba55eb3d112189ac198c3066d487a45e3b4d4c"
command -v curl >/dev/null
command -v unzip >/dev/null
command -v sha256sum >/dev/null
echo "Downloading official LiteRT-LM C API 1.0.0 Android SDK..."
curl --fail --location --retry 3 --retry-delay 2 "$URL" --output "$ZIP_PATH"
echo "$SHA256  $ZIP_PATH" | sha256sum --check -
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
unzip -q "$ZIP_PATH" -d "$TMP_DIR"
LIB_PATH="$(find "$TMP_DIR" -type f -name 'libLiteRtLm.so' -print -quit)"
ENGINE_HEADER="$(find "$TMP_DIR" -type f -path '*/c/engine.h' -print -quit)"
if [[ -z "$LIB_PATH" || -z "$ENGINE_HEADER" ]]; then
  echo "LiteRT-LM archive did not contain the expected Android library and C headers." >&2
  find "$TMP_DIR" -maxdepth 5 -type f | head -100 >&2
  exit 1
fi
HEADER_ROOT="${ENGINE_HEADER%/c/engine.h}"
rm -rf "$SDK_DIR"
mkdir -p "$SDK_DIR/include" "$SDK_DIR/lib/arm64-v8a"
cp -a "$HEADER_ROOT/c" "$SDK_DIR/include/"
cp "$LIB_PATH" "$SDK_DIR/lib/arm64-v8a/libLiteRtLm.so"
test -s "$SDK_DIR/include/c/engine.h"
test -s "$SDK_DIR/include/c/conversation.h"
test -s "$SDK_DIR/lib/arm64-v8a/libLiteRtLm.so"
echo "LiteRT-LM Android arm64 SDK prepared."
du -h "$SDK_DIR/lib/arm64-v8a/libLiteRtLm.so"
