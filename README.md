# DevAI

**An original, offline-first Android AI research project.**

DevAI is being developed as an independently implemented language model. Its tokenizer, neural-network architecture, initialization, training loop, and learned weights are intended to be created in this repository. It does not use pretrained model weights or external AI APIs.

> Current status: Milestone 2 — experimental byte-level BPE tokenizer. The Android app can train merge rules from text entered by the user, encode text into token IDs, and decode those IDs. This is a tokenizer experiment, not a trained language model. Transformer inference and neural-network training are not implemented yet.

## Principles

- No pretrained model weights (Qwen, Llama, Gemma, DeepSeek, etc.).
- No external AI inference service or API key.
- Native Android app using Java + XML.
- C++17 core connected through JNI.
- Train model weights from random initialization.
- Use documented datasets with appropriate licenses.
- Run inference locally once a model has actually been trained.

## Current implementation

- Android Java activity and XML interface.
- CMake-built C++ shared library.
- JNI bridge for tokenizer operations.
- Deterministic byte-level BPE merge training from user-provided text.
- Token encode/decode using the learned merge list.
- Host-side tokenizer tests and a standalone test runner.
- No INTERNET permission and no network inference dependencies.

### Run native tokenizer tests

On a machine with a C++17 compiler:

```sh
sh tests/run_tokenizer_tests.sh
```

### Tokenizer limitations

This is an educational first BPE implementation. It learns at most 128 merges from the text pasted into the app; it does not yet persist vocabulary files, handle large corpora efficiently, or implement a production-grade Unicode normalization/pre-tokenization pipeline. JNI currently uses Android's modified UTF-8 string interface, so supplementary Unicode characters need dedicated boundary tests before claiming full Unicode correctness.

Tokenizer training does not train neural-network weights. The app is not yet a conversational AI.

## Build requirements

- Android Studio
- Android SDK Platform 35
- Android NDK (side-by-side)
- CMake 3.22.1
- JDK 17
- Android Gradle Plugin 8.6.1 / compatible Gradle 8.x

Open the project in Android Studio, install the requested SDK/NDK components, sync Gradle, and build the app debug variant.

- Minimum Android: API 27 (Android 8.1)
- Initial ABI: arm64-v8a
- No Gradle wrapper is included yet.

## Roadmap

1. [x] Android/JNI foundation and byte encoding prototype.
2. [x] Experimental byte-level BPE training and encode/decode.
3. [x] Initial host-side tokenizer tests.
4. [ ] Persist tokenizer vocabulary and merge ranks.
5. [ ] Implement a tiny decoder-only Transformer forward pass.
6. [ ] Implement gradients, AdamW, gradient clipping, and checkpointing.
7. [ ] Add licensed training/validation datasets and reproducible training configs.
8. [ ] Train a small randomly initialized model and report measured validation loss.
9. [ ] Add local autoregressive inference and connect it to chat.
10. [ ] Profile memory, speed, battery and thermals on physical Android devices.

## Scaling reality

A 100M-parameter model is a long-term research target, not a realistic first training run on a mid-range Android phone. Start with tiny configurations, measure peak memory and sustained throughput, checkpoint frequently, and scale only when measured resources permit. Parameter count alone does not imply useful intelligence.

## Model integrity

- Initialize all model weights randomly.
- Never import or convert pretrained weights.
- Record dataset provenance, license, preprocessing and training configuration.
- Keep validation data out of training.
- Report actual tests and measured results; do not claim capabilities that have not been demonstrated.

## License

No project license has been selected yet. Until one is added, this repository does not grant an open-source license.
