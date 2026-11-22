import json
import tempfile
import unittest
from pathlib import Path

from prepare_dataset import concept_group, load_records, split_records, write_jsonl


def make_record(record_id: str, language: str) -> dict:
    return {
        "id": record_id,
        "kind": "knowledge",
        "language": language,
        "level": "beginner",
        "prompt": f"question {record_id}",
        "response": f"answer {record_id}",
        "tags": ["test"],
        "provenance": "test",
    }


class DatasetPreparationTests(unittest.TestCase):
    def test_translated_variants_share_group(self):
        self.assertEqual(concept_group("gravity-en"), "gravity")
        self.assertEqual(concept_group("gravity-hi"), "gravity")
        self.assertEqual(concept_group("gravity-ur"), "gravity")

    def test_split_is_deterministic_and_has_no_group_leakage(self):
        records = [
            make_record(f"{concept}-{language}", language)
            for concept in ("gravity", "water", "moon", "heart", "earth")
            for language in ("en", "hi", "ur")
        ]
        train_a, validation_a = split_records(records, 0.2, 1234)
        train_b, validation_b = split_records(records, 0.2, 1234)
        self.assertEqual(train_a, train_b)
        self.assertEqual(validation_a, validation_b)
        train_groups = {concept_group(row["id"]) for row in train_a}
        validation_groups = {concept_group(row["id"]) for row in validation_a}
        self.assertFalse(train_groups & validation_groups)
        self.assertTrue(train_a)
        self.assertTrue(validation_a)

    def test_split_preserves_all_records(self):
        records = [make_record(f"topic-{i}-en", "en") for i in range(12)]
        train, validation = split_records(records, 0.25, 7)
        self.assertEqual(len(train) + len(validation), len(records))
        self.assertEqual({r["id"] for r in train + validation}, {r["id"] for r in records})

    def test_invalid_ratio_is_rejected(self):
        with self.assertRaises(ValueError):
            split_records([make_record("a-en", "en"), make_record("b-en", "en")], 0, 1)

    def test_duplicate_ids_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "data.jsonl"
            record = make_record("same-en", "en")
            path.write_text(json.dumps(record) + "\n" + json.dumps(record) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate id"):
                load_records(path)

    def test_jsonl_round_trip_keeps_unicode(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "nested" / "out.jsonl"
            records = [make_record("namaste-hi", "hi")]
            records[0]["prompt"] = "नमस्ते"
            write_jsonl(path, records)
            self.assertIn("नमस्ते", path.read_text(encoding="utf-8"))
            self.assertEqual(load_records(path), records)


if __name__ == "__main__":
    unittest.main()
