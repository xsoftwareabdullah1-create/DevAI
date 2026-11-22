#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/devai-tokenizer-test"
CXX="${CXX:-c++}"
"$CXX" -std=c++17 -Wall -Wextra -Werror \
  "$ROOT/app/src/main/cpp/tokenizer.cpp" \
  "$ROOT/tests/tokenizer_test.cpp" \
  -o "$OUT"
"$OUT"
