#!/usr/bin/env python3
"""Offline Dev Orus 1.1 retrieval CLI.

This mirrors the character n-gram TF-IDF retrieval baseline used by the Android
native bridge. It retrieves stored answers; it is not a generative language model.
"""
from __future__ import annotations

import argparse
import json
import math
import os
from collections import Counter
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CORPUS = ROOT / "app" / "src" / "main" / "assets" / "devai_corpus.tsv"
DEFAULT_HISTORY = Path.home() / ".devorus" / "history.jsonl"
UNKNOWN = (
    "I don't have a reliable match for that yet. My current offline knowledge "
    "is limited, and I don't want to guess."
)


def make_ngrams(text: str) -> Counter[bytes]:
    # Match native_bridge.cpp: lowercase ASCII only, then 2-4 byte UTF-8 n-grams.
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
            if not terms:
                continue
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
            for line_number, line in enumerate(handle, 1):
                line = line.rstrip("\r\n")
                if "\t" not in line:
                    continue
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
        q_weights: dict[bytes, float] = {}
        for term, frequency in terms.items():
            if term in self.idf:
                q_weights[term] = (1.0 + math.log(frequency)) * self.idf[term]
        q_norm = math.sqrt(sum(weight * weight for weight in q_weights.values()))
        if q_norm <= 0.0:
            return ("I don't have a close match in my current offline knowledge yet.",
                    None, 0.0)
        best_score = 0.0
        best_doc: Document | None = None
        for doc in self.documents:
            dot = sum(weight * doc.weights.get(term, 0.0)
                      for term, weight in q_weights.items())
            score = dot / (q_norm * doc.norm) if doc.norm > 0 else 0.0
            if score > best_score:
                best_score, best_doc = score, doc
        if best_doc is None or best_score < 0.10:
            return UNKNOWN, best_doc.prompt if best_doc else None, best_score
        return best_doc.answer, best_doc.prompt, best_score


def save_history(path: Path, question: str, answer: str, score: float,
                 matched_prompt: str | None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    item = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "question": question,
        "answer": answer,
        "score": round(score, 6),
        "matched_prompt": matched_prompt,
    }
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(item, ensure_ascii=False) + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Dev Orus 1.1 offline response tester")
    parser.add_argument("--corpus", type=Path, default=DEFAULT_CORPUS)
    parser.add_argument("--history", type=Path, default=DEFAULT_HISTORY)
    parser.add_argument("--ask", help="ask one question and exit")
    args = parser.parse_args()

    try:
        model = Retriever.from_tsv(args.corpus)
    except (OSError, UnicodeError, ValueError) as exc:
        print(f"Could not load Dev Orus knowledge: {exc}")
        return 1

    print("Dev Orus 1.1 · Offline response tester")
    print(f"Loaded {len(model.documents)} local prompt/response records.")
    print("Retrieval baseline only — it selects stored answers, not new generated text.")
    print("Commands: /help  /history  /exit")
    if args.ask is not None:
        questions = [args.ask]
    else:
        questions = None

    def respond(question: str) -> None:
        answer, matched, score = model.ask(question)
        print(f"\nDev Orus: {answer}")
        print(f"[match score: {score:.3f}]")
        if matched:
            print(f"[matched example: {matched}]")
        save_history(args.history, question, answer, score, matched)
        print()

    if questions is not None:
        for question in questions:
            respond(question)
        return 0

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
            print("Type a question. /history shows saved turns; /exit closes the chat.")
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
