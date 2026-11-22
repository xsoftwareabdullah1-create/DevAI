#!/usr/bin/env python3
"""Create deterministic train/validation JSONL splits without language leakage."""
from __future__ import annotations

import argparse
import json
import random
import re
from collections import defaultdict
from pathlib import Path
from typing import Any

LANGUAGE_SUFFIX = re.compile(r"-(?:en|hi|ur)$")
REQUIRED_FIELDS = {"id", "kind", "language", "level", "prompt", "response", "tags", "provenance"}


def concept_group(record_id: str) -> str:
    """Group translated variants of one concept so they stay in one split."""
    return LANGUAGE_SUFFIX.sub("", record_id)


def load_records(path: Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    seen: set[str] = set()
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            raise ValueError(f"line {line_number}: blank lines are not allowed")
        try:
            item = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError(f"line {line_number}: invalid JSON: {exc}") from exc
        if not isinstance(item, dict):
            raise ValueError(f"line {line_number}: record must be an object")
        missing = REQUIRED_FIELDS - item.keys()
        if missing:
            raise ValueError(f"line {line_number}: missing fields {sorted(missing)}")
        record_id = item["id"]
        if not isinstance(record_id, str) or not record_id.strip():
            raise ValueError(f"line {line_number}: id must be a non-empty string")
        if record_id in seen:
            raise ValueError(f"line {line_number}: duplicate id {record_id!r}")
        seen.add(record_id)
        if not isinstance(item["prompt"], str) or not item["prompt"].strip():
            raise ValueError(f"line {line_number}: prompt must be non-empty")
        if not isinstance(item["response"], str) or not item["response"].strip():
            raise ValueError(f"line {line_number}: response must be non-empty")
        records.append(item)
    if len(records) < 2:
        raise ValueError("at least two records are required to create a split")
    return records


def split_records(
    records: list[dict[str, Any]], validation_ratio: float, seed: int
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    if not 0.0 < validation_ratio < 1.0:
        raise ValueError("validation ratio must be greater than 0 and less than 1")

    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for record in records:
        groups[concept_group(record["id"])].append(record)

    group_names = sorted(groups)
    if len(group_names) < 2:
        raise ValueError("at least two distinct concept groups are required")

    shuffled = group_names[:]
    random.Random(seed).shuffle(shuffled)
    validation_count = max(1, round(len(shuffled) * validation_ratio))
    validation_count = min(validation_count, len(shuffled) - 1)
    validation_groups = set(shuffled[:validation_count])

    train = [record for record in records if concept_group(record["id"]) not in validation_groups]
    validation = [record for record in records if concept_group(record["id"]) in validation_groups]
    return train, validation


def write_jsonl(path: Path, records: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "".join(json.dumps(record, ensure_ascii=False, sort_keys=True) + "\n" for record in records),
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path("data/knowledge/seed_knowledge.jsonl"))
    parser.add_argument("--train-output", type=Path, default=Path("build/dataset/train.jsonl"))
    parser.add_argument("--validation-output", type=Path, default=Path("build/dataset/validation.jsonl"))
    parser.add_argument("--validation-ratio", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=2026)
    args = parser.parse_args()

    try:
        records = load_records(args.input)
        train, validation = split_records(records, args.validation_ratio, args.seed)
        write_jsonl(args.train_output, train)
        write_jsonl(args.validation_output, validation)
    except (OSError, ValueError) as exc:
        parser.error(str(exc))

    print(
        f"Prepared deterministic split: {len(train)} train records, "
        f"{len(validation)} validation records; seed={args.seed}"
    )
    print("Translated variants of the same concept are kept in the same split.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
