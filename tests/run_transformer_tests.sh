#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/devai-transformer-test"
CXX="${CXX:-c++}"
"$CXX" -std=c++17 -O2 -Wall -Wextra -Werror \
 "$ROOT/app/src/main/cpp/transformer.cpp" "$ROOT/tests/transformer_test.cpp" -o "$OUT"
"$OUT"
