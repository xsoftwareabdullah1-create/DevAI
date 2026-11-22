import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from io import BytesIO

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from devorus_cli import LlamaServer, Retriever, make_ngrams, save_history


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
            self.assertIn('"backend": "retrieval"', content)

    def test_local_llama_server_response_is_parsed(self):
        payload = {"choices": [{"message": {"content": "Hello from local model"}}]}
        response = BytesIO(json.dumps(payload).encode("utf-8"))
        client = LlamaServer("http://127.0.0.1:8080", "You are Dev Orus.")
        with patch("urllib.request.urlopen", return_value=response):
            answer = client.ask("Hello", [])
        self.assertEqual(answer, "Hello from local model")


if __name__ == "__main__":
    unittest.main()
