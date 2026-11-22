#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK_DIR="$ROOT/app/src/main/cpp/third_party/litertlm"
ZIP_PATH="/tmp/litert_lm_c_api-1.0.0.zip"
URL="https://github.com/google-ai-edge/LiteRT-LM/releases/download/v0.19.1/litert_lm_c_api-1.0.0.zip"
SHA256="1d430d80eaf536f696a4922270ba55eb3d112189ac198c3066d487a45e3b4d4c"
command -v curl >/dev/null
command -v unzip >/dev/null
command -v sha256sum >/dev/null
command -v file >/dev/null
echo "Downloading official LiteRT-LM C API 1.0.0 Android SDK..."
curl --fail --location --retry 3 --retry-delay 2 "$URL" --output "$ZIP_PATH"
echo "$SHA256  $ZIP_PATH" | sha256sum --check -
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
unzip -q "$ZIP_PATH" -d "$TMP_DIR"

LIB_PATH=""
while IFS= read -r candidate; do
  if file "$candidate" | grep -q "ARM aarch64"; then
    LIB_PATH="$candidate"
    break
  fi
done < <(find "$TMP_DIR" -type f -iname '*litert*lm*.so' -print)
if [[ -z "$LIB_PATH" ]]; then
  echo "LiteRT-LM archive did not contain an ARM64 Android shared library." >&2
  find "$TMP_DIR" -type f -name '*.so' -exec file {} \; >&2
  exit 1
fi

rm -rf "$SDK_DIR"
mkdir -p "$SDK_DIR/include/c" "$SDK_DIR/lib/arm64-v8a"
cp "$LIB_PATH" "$SDK_DIR/lib/arm64-v8a/libLiteRtLm.so"

# Pin headers to the same official runtime release.
for header in api_export.h error_reporter.h engine.h conversation.h; do
  curl --fail --location --retry 3 \
    "https://raw.githubusercontent.com/google-ai-edge/LiteRT-LM/v0.19.1/c/$header" \
    --output "$SDK_DIR/include/c/$header"
done

test -s "$SDK_DIR/include/c/engine.h"
test -s "$SDK_DIR/include/c/conversation.h"
test -s "$SDK_DIR/lib/arm64-v8a/libLiteRtLm.so"
# Package the shared dependency where Android Gradle collects JNI libraries.\nJNI_LIB_DIR="$ROOT/app/src/main/jniLibs/arm64-v8a"\nmkdir -p "$JNI_LIB_DIR"\ninstall -m 0644 "$SDK_DIR/lib/arm64-v8a/libLiteRtLm.so" "$JNI_LIB_DIR/libLiteRtLm.so"\n\necho "LiteRT-LM Android arm64 SDK and APK JNI runtime prepared."
du -h "$SDK_DIR/lib/arm64-v8a/libLiteRtLm.so"
