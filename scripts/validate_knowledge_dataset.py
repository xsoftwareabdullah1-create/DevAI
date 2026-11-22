#!/usr/bin/env python3
"""Validate DevAI's small UTF-8 JSONL starter knowledge corpus."""
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "knowledge" / "seed_knowledge.jsonl"
ALLOWED_KINDS = {"knowledge", "conversation"}
ALLOWED_LANGUAGES = {"en", "hi", "ur"}
REQUIRED = {"id", "kind", "language", "level", "prompt", "response", "tags", "provenance"}


def main() -> int:
    seen = set()
    counts = Counter()
    errors = []
    try:
        lines = DATA.read_text(encoding="utf-8").splitlines()
    except OSError as exc:
        print(f"ERROR: cannot read {DATA}: {exc}")
        return 1

    for line_number, line in enumerate(lines, start=1):
        if not line.strip():
            errors.append(f"line {line_number}: blank lines are not allowed")
            continue
        try:
            item = json.loads(line)
        except json.JSONDecodeError as exc:
            errors.append(f"line {line_number}: invalid JSON: {exc}")
            continue
        if not isinstance(item, dict):
            errors.append(f"line {line_number}: record must be a JSON object")
            continue
        missing = REQUIRED - item.keys()
        if missing:
            errors.append(f"line {line_number}: missing fields: {', '.join(sorted(missing))}")
            continue
        if not all(isinstance(item[key], str) and item[key].strip()
                   for key in ("id", "kind", "language", "level", "prompt", "response", "provenance")):
            errors.append(f"line {line_number}: required text fields must be non-empty strings")
            continue
        if item["id"] in seen:
            errors.append(f"line {line_number}: duplicate id {item['id']!r}")
        seen.add(item["id"])
        if item["kind"] not in ALLOWED_KINDS:
            errors.append(f"line {line_number}: unsupported kind {item['kind']!r}")
        if item["language"] not in ALLOWED_LANGUAGES:
            errors.append(f"line {line_number}: unsupported starter language {item['language']!r}")
        if "response_style" in item and item["response_style"] not in {"concise", "detailed"}:
            errors.append(f"line {line_number}: response_style must be concise or detailed")
        if "context" in item:
            context = item["context"]
            if item["kind"] != "conversation":
                errors.append(f"line {line_number}: context is only valid for conversation records")
            if not isinstance(context, list) or not context or len(context) > 12:
                errors.append(f"line {line_number}: context must be a non-empty list of at most 12 messages")
            else:
                expected_role = "user"
                for message_index, message in enumerate(context, start=1):
                    if not isinstance(message, dict) or set(message) != {"role", "content"}:
                        errors.append(f"line {line_number}: context message {message_index} must contain role and content only")
                        continue
                    if message["role"] != expected_role:
                        errors.append(f"line {line_number}: context message {message_index} must have role {expected_role!r}")
                    if not isinstance(message["content"], str) or not message["content"].strip():
                        errors.append(f"line {line_number}: context message {message_index} content must be non-empty text")
                    expected_role = "assistant" if expected_role == "user" else "user"
                if context[-1].get("role") != "assistant":
                    errors.append(f"line {line_number}: context must end with an assistant message before the current prompt")
        if not isinstance(item["tags"], list) or not item["tags"] or not all(
            isinstance(tag, str) and tag.strip() for tag in item["tags"]
        ):
            errors.append(f"line {line_number}: tags must be a non-empty list of strings")
        counts[(item["kind"], item["language"])] += 1

    if not lines:
        errors.append("dataset is empty")
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1

    print(f"DevAI knowledge dataset valid: {len(lines)} records")
    for (kind, language), count in sorted(counts.items()):
        print(f"  {kind}/{language}: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
