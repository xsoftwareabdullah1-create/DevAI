import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from devorus_cli import Retriever, make_ngrams, save_history


class DevOrusCliTests(unittest.TestCase):
    def test_english_prompt_retrieves_stored_answer(self):
        model = Retriever([("What is Python?", "Python is a programming language.")])
        answer, matched, score = model.ask("What is Python?")
        self.assertEqual(answer, "Python is a programming language.")
        self.assertEqual(matched, "What is Python?")
        self.assertGreaterEqual(score, 0.10)

    def test_unrelated_question_returns_unknown(self):
        model = Retriever([("What is Python?", "Python is a programming language.")])
        answer, _, _ = model.ask("zzzz unrelated moon question")
        self.assertIn("don't have a reliable match", answer)

    def test_unicode_ngrams_are_supported(self):
        self.assertTrue(make_ngrams("پائتھن کیا ہے؟"))
        self.assertTrue(make_ngrams("पाइथन क्या है?"))

    def test_history_is_saved_as_unicode_jsonl(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "history.jsonl"
            save_history(path, "پائتھن کیا ہے؟", "یہ جواب ہے", 0.5, "سوال")
            content = path.read_text(encoding="utf-8")
            self.assertIn("پائتھن", content)
            self.assertIn("یہ جواب ہے", content)


if __name__ == "__main__":
    unittest.main()
