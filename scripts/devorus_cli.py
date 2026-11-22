#!/usr/bin/env python3
"""Dev Orus 1.1 CLI: local corpus retrieval or a fully local llama.cpp server.

The llama-server backend generates text with a separately downloaded model.
The retrieval backend only selects stored corpus responses.
"""
from __future__ import annotations

import argparse
import json
import math
import urllib.error
import urllib.request
from collections import Counter
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CORPUS = ROOT / "app" / "src" / "main" / "assets" / "devai_corpus.tsv"
DEFAULT_HISTORY = Path.home() / ".devorus" / "history.jsonl"
DEFAULT_SYSTEM_PROMPT = (
    "You are Dev Orus 1.1, an offline assistant running locally on the user's device. "
    "Answer clearly and honestly. If you are unsure, say so. Prefer the language used "
    "by the user (Hindi, Urdu, or English). Do not claim to have internet access."
)
UNKNOWN = (
    "I don't have a reliable match for that yet. My current offline knowledge "
    "is limited, and I don't want to guess."
)


def make_ngrams(text: str) -> Counter[bytes]:
    raw = bytearray(text.encode("utf-8"))
    for i, value in enumerate(raw):
        if value < 128 and 65 <= value <= 90:
            raw[i] = value + 32
    terms: Counter[bytes] = Counter()
    for n in (2, 3, 4):
        if len(raw) < n:
            continue
        for i in range(len(raw) - n + 1):
            gram = bytes(raw[i:i + n])
            if not all(chr(c).isspace() for c in gram):
                terms[gram] += 1
    return terms


@dataclass
class Document:
    prompt: str
    answer: str
    weights: dict[bytes, float]
    norm: float


class Retriever:
    def __init__(self, records: Iterable[tuple[str, str]]) -> None:
        raw_docs: list[tuple[str, str, Counter[bytes]]] = []
        df: Counter[bytes] = Counter()
        for prompt, answer in records:
            terms = make_ngrams(prompt)
            if terms:
                raw_docs.append((prompt, answer, terms))
                df.update(terms.keys())
        if not raw_docs:
            raise ValueError("No valid prompt/response records found.")
        count = float(len(raw_docs))
        idf = {term: math.log(1.0 + (count + 1.0) / (freq + 1.0))
               for term, freq in df.items()}
        self.documents: list[Document] = []
        for prompt, answer, terms in raw_docs:
            weights = {term: (1.0 + math.log(freq)) * idf[term]
                       for term, freq in terms.items()}
            norm = math.sqrt(sum(weight * weight for weight in weights.values()))
            self.documents.append(Document(prompt, answer, weights, norm))
        self.idf = idf

    @classmethod
    def from_tsv(cls, path: Path) -> "Retriever":
        records: list[tuple[str, str]] = []
        with path.open("r", encoding="utf-8") as handle:
            for line in handle:
                line = line.rstrip("\r\n")
                if "\t" in line:
                    prompt, answer = line.split("\t", 1)
                    if prompt and answer:
                        records.append((prompt, answer))
        if not records:
            raise ValueError(f"No valid records in corpus: {path}")
        return cls(records)

    def ask(self, question: str) -> tuple[str, str | None, float]:
        terms = make_ngrams(question)
        if not terms:
            return ("Please write a little more so I can understand your question.",
                    None, 0.0)
        q_weights = {
            term: (1.0 + math.log(freq)) * self.idf[term]
            for term, freq in terms.items() if term in self.idf
        }
        q_norm = math.sqrt(sum(weight * weight for weight in q_weights.values()))
        if q_norm <= 0.0:
            return ("I don't have a close match in my current offline knowledge yet.",
                    None, 0.0)
        best_score = 0.0
        best_doc: Document | None = None
        for doc in self.documents:
            dot = sum(weight * doc.weights.get(term, 0.0)
                      for term, weight in q_weights.items())
            score = dot / (q_norm * doc.norm) if doc.norm else 0.0
            if score > best_score:
                best_score, best_doc = score, doc
        if best_doc is None or best_score < 0.10:
            return UNKNOWN, best_doc.prompt if best_doc else None, best_score
        return best_doc.answer, best_doc.prompt, best_score


class LlamaServer:
    """Talk to llama.cpp's OpenAI-compatible API on localhost only by default."""

    def __init__(self, base_url: str, system_prompt: str, timeout: int = 180) -> None:
        self.base_url = base_url.rstrip("/")
        self.system_prompt = system_prompt
        self.timeout = timeout

    def ask(self, question: str, history: list[dict[str, str]]) -> str:
        messages = [{"role": "system", "content": self.system_prompt}]
        messages.extend(history[-12:])
        messages.append({"role": "user", "content": question})
        payload = json.dumps({
            "messages": messages,
            "temperature": 0.7,
            "max_tokens": 384,
            "stream": False,
        }).encode("utf-8")
        request = urllib.request.Request(
            self.base_url + "/v1/chat/completions",
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                result = json.loads(response.read().decode("utf-8"))
            answer = result["choices"][0]["message"]["content"]
            if not isinstance(answer, str) or not answer.strip():
                raise ValueError("The local model returned an empty response.")
            return answer.strip()
        except urllib.error.URLError as exc:
            raise RuntimeError(
                "Cannot reach the local llama-server. Start it on 127.0.0.1:8080 "
                "and check that the model loaded successfully."
            ) from exc
        except (KeyError, IndexError, TypeError, json.JSONDecodeError) as exc:
            raise RuntimeError("The local model server returned an invalid response.") from exc


def save_history(path: Path, question: str, answer: str,
                 score: float | None, matched_prompt: str | None,
                 backend: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    item = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "backend": backend,
        "question": question,
        "answer": answer,
        "score": round(score, 6) if score is not None else None,
        "matched_prompt": matched_prompt,
    }
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(item, ensure_ascii=False) + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Dev Orus 1.1 local chat")
    parser.add_argument("--backend", choices=("retrieval", "llama-server"),
                        default="retrieval")
    parser.add_argument("--corpus", type=Path, default=DEFAULT_CORPUS)
    parser.add_argument("--history", type=Path, default=DEFAULT_HISTORY)
    parser.add_argument("--ask", help="ask one question and exit")
    parser.add_argument("--server", default="http://127.0.0.1:8080",
                        help="local llama-server URL")
    parser.add_argument("--system-prompt", default=DEFAULT_SYSTEM_PROMPT)
    args = parser.parse_args()

    retriever: Retriever | None = None
    llm: LlamaServer | None = None
    if args.backend == "retrieval":
        try:
            retriever = Retriever.from_tsv(args.corpus)
        except (OSError, UnicodeError, ValueError) as exc:
            print(f"Could not load Dev Orus knowledge: {exc}")
            return 1
    else:
        llm = LlamaServer(args.server, args.system_prompt)

    print("Dev Orus 1.1 · Offline chat")
    if retriever:
        print(f"Backend: local TF-IDF retrieval · {len(retriever.documents)} records")
        print("This mode selects stored answers; it does not generate new text.")
    else:
        print(f"Backend: local llama.cpp server · {args.server}")
        print("Generated locally by the model file on this device; no cloud API.")
    print("Commands: /help  /history  /clear  /exit")

    conversation: list[dict[str, str]] = []

    def respond(question: str) -> bool:
        if retriever:
            answer, matched, score = retriever.ask(question)
            print(f"\nDev Orus: {answer}")
            print(f"[retrieval match score: {score:.3f}]")
            if matched:
                print(f"[matched example: {matched}]")
            save_history(args.history, question, answer, score, matched, "retrieval")
        else:
            assert llm is not None
            try:
                answer = llm.ask(question, conversation)
            except RuntimeError as exc:
                print(f"\nDev Orus local model error: {exc}\n")
                return False
            print(f"\nDev Orus: {answer}\n")
            conversation.extend([
                {"role": "user", "content": question},
                {"role": "assistant", "content": answer},
            ])
            save_history(args.history, question, answer, None, None, "llama-server")
        print()
        return True

    if args.ask is not None:
        return 0 if respond(args.ask) else 1

    while True:
        try:
            question = input("You: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nDev Orus session closed.")
            break
        if not question:
            continue
        command = question.lower()
        if command in {"/exit", "/quit"}:
            print("Dev Orus session closed.")
            break
        if command == "/help":
            print("Ask a question. /history shows saved turns; /clear resets this session; /exit closes.")
            continue
        if command == "/clear":
            conversation.clear()
            print("Current conversation context cleared.")
            continue
        if command == "/history":
            if not args.history.exists():
                print("No saved conversations yet.")
            else:
                with args.history.open("r", encoding="utf-8") as handle:
                    entries = list(handle)
                for line in entries[-10:]:
                    try:
                        item = json.loads(line)
                        print(f"You: {item['question']}\nDev Orus: {item['answer']}\n")
                    except (json.JSONDecodeError, KeyError):
                        continue
            continue
        respond(question)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
