# DevAI starter knowledge corpus

This is a original, hand-authored starter corpus for testing the future local training pipeline. It contains beginner general-knowledge question/answer pairs and ordinary conversational examples (greetings, thanks, jokes, riddles, boredom and playful nonsense) in English, Hindi and Urdu.

## Format

Each UTF-8 JSONL line is one independent record:
- `id`: stable unique record identifier
- `kind`: `knowledge` or `conversation`
- `language`: BCP-47 primary language code (`en`, `hi`, `ur`)
- `level`: curriculum level
- `prompt` / `response`: sample exchange
- `tags`: topical labels
- `provenance`: `original_seed` means written for this repository, not copied from a third-party dataset

## Validate

Run from the repository root:

```sh
python3 scripts/validate_knowledge_dataset.py
```

## Important limitations

- This starter set is deliberately tiny and is not enough to teach a useful language model.
- Adding text to this folder does not update model weights. DevAI still needs a real training loop (loss, gradients, optimizer, batching, validation and checkpointing) before it can learn from these records.
- The examples cover only English, Hindi and Urdu for now. The data format is Unicode-friendly; additional languages should be added in reviewed, correctly labeled, licensed batches.
- General-knowledge facts should be checked and expanded before large-scale training. Add source URLs and license metadata for any future third-party corpus; do not copy books, websites or datasets without permission.
- Casual or silly questions should receive friendly, harmless responses rather than being dismissed as “stupid”. The model should ask a clarifying question when a prompt is unclear and should not invent factual answers.


## Deterministic split preparation

To prepare local train and validation JSONL files without putting translated versions of the same concept into both sets:

```sh
python3 scripts/prepare_dataset.py --seed 2026
```

By default this reads the seed corpus and writes generated files under `build/dataset/`. The utility groups IDs ending in `-en`, `-hi` and `-ur` by their shared concept prefix before splitting. This prevents a translated version of a validation example from leaking into training. The default validation ratio is 10%; the seed makes the split reproducible.

This is data preparation only. The Transformer still has no gradient calculation, optimizer or training loop, so the split files do not teach the model yet.
