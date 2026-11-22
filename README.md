# DevAI

**An original, offline-first Android AI research project.**

DevAI is an independently implemented language-model research project. It uses no pretrained model weights and no external AI inference APIs. The current code includes an experimental tokenizer, a small C++ decoder-only Transformer forward pass, and an original multilingual starter corpus.

> **Current status: forward-pass prototype + starter dataset.** The app can train a byte-level BPE tokenizer from supplied text, encode/decode IDs, and run a tiny randomly initialized Transformer. A 105-record beginner corpus is now included for future training-pipeline work. The model has **not** learned this corpus yet and does not produce meaningful chat.

## Implemented

- Android Java + XML interface; C++17 core through JNI.
- Byte-level BPE tokenizer training, encode and decode.
- Decoder-only Transformer forward pass with token embeddings, RMSNorm, causal multi-head self-attention, RoPE, SwiGLU feed-forward layers, residual connections and vocabulary logits.
- Deterministic random initialization from a configurable seed.
- Input validation and a 2,000,000-parameter allocation guard.
- Original 105-record starter corpus: beginner general knowledge and casual conversation in English, Hindi and Urdu.
- Python standard-library validator for the JSONL corpus.
- No INTERNET permission, external inference API, or pretrained weights.

## Starter knowledge and casual conversation data

See [data/knowledge/README.md](data/knowledge/README.md) and [seed_knowledge.jsonl](data/knowledge/seed_knowledge.jsonl).

The starter records include basic science, geography, mathematics and biology, plus friendly handling of greetings, thanks, jokes, riddles, boredom and playful/nonsensical prompts. The intended behavior is to respond politely to casual questions instead of dismissing them, while not inventing factual answers.

Validate the dataset:

```sh
python3 scripts/validate_knowledge_dataset.py
```

The starter corpus currently covers English, Hindi and Urdu only, with 35 concepts represented in each language. It includes beginner science, geography, mathematics, biology, environment, technology, civics and history. It is a small original seed set, not a comprehensive general-knowledge library. More languages and carefully reviewed, properly licensed data need to be added gradually.

## Tiny Android configuration

The UI demo uses a deliberately small configuration: vocabulary 512, width 32, 4 attention heads, 1 layer, SwiGLU width 64, maximum sequence length 64. Parameters are randomly initialized each time the demo runs. This configuration validates tensor flow, not capability or quality.

## Native tests

Requires a C++17 compiler:

```sh
sh tests/run_tokenizer_tests.sh
sh tests/run_transformer_tests.sh
python3 scripts/validate_knowledge_dataset.py
```

Transformer tests cover finite logits, causal prefix invariance, deterministic initialization, seed sensitivity, invalid IDs, sequence bounds, and invalid head dimensions.

## Important limitations

- No backpropagation, optimizer, training loop, dataset batching, or learned checkpoint exists yet.
- Adding corpus files does not teach the current model. The Android forward-pass button reports the highest final-position logit only; it does not decode a response or constitute a chat assistant.
- Weights are reinitialized on each JNI call; they are not persisted.
- The starter corpus is intentionally small and is insufficient for useful language-model training.
- Multilingual expansion requires correctly labeled examples and language-specific evaluation. “All languages” is a long-term coverage goal, not a current capability.
- This is a correctness-oriented scalar CPU reference, not optimized for NEON, multithreading, quantization, or long contexts.
- Attention uses a simple causal loop and has quadratic sequence-time complexity.
- The BPE vocabulary is held in process memory; persistence and production Unicode handling remain future work.
- Actual runtime RAM, speed, battery and thermals still require physical-device profiling.

## Build requirements

- Android Studio, Android SDK Platform 35
- Android NDK and CMake 3.22.1
- JDK 17 and Android Gradle Plugin 8.6.1
- Initial ABI: arm64-v8a; minimum Android API 27
- No Gradle wrapper is currently included.

## Roadmap

1. [x] Android/JNI foundation.
2. [x] Experimental byte-level BPE tokenizer.
3. [x] Tiny decoder-only Transformer forward pass.
4. [x] Basic native correctness tests.
5. [x] Original beginner knowledge + casual conversation starter corpus (English/Hindi/Urdu).
6. [x] JSONL dataset validator and CI validation.
7. [ ] Persist tokenizer vocabulary and model checkpoints.
8. [ ] Implement gradients, backpropagation, AdamW and gradient clipping.
9. [ ] Add licensed multilingual dataset pipeline, curriculum levels and reproducible train/validation splits.
10. [ ] Train a tiny randomly initialized model and report held-out loss.
11. [ ] Add autoregressive generation and real chat only after training.
12. [ ] Expand language coverage and evaluate each language independently.
13. [ ] Profile and optimize for Android CPU, RAM, battery and thermal limits.

## Scaling reality

A 100M-parameter model is a long-term research target, not a realistic first training run on a mid-range Android phone. Begin with tiny configurations and scale only from measured memory and sustained throughput. Parameter count alone does not imply useful intelligence.

## Model integrity

- Initialize all model weights randomly.
- Never import or convert pretrained weights.
- Record dataset provenance, license, preprocessing and training configuration.
- Keep validation data out of training.
- Report actual tests and measured results; do not claim capabilities that have not been demonstrated.

## License

No project license has been selected yet. Until one is added, this repository does not grant an open-source license.
