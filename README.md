# DevAI

**An original, offline-first Android AI research project.**

DevAI is intended to become a language model whose tokenizer, architecture, initialization, training loop, and learned weights are developed in this repository. It will not ship with or depend on pretrained weights from Qwen, Llama, Gemma, DeepSeek, or another model.

> **Current status: early foundation only.** The current app is a native Android shell with a C++ JNI bridge and a reversible byte-tokenizer demonstration. It is **not yet an AI chatbot**. There is no Transformer, training loop, optimizer, trained model, or model inference in this first milestone.

## Goals

- Native Android app using **Java + XML**.
- Native compute core in **C++17**, connected through JNI.
- Build an original tokenizer from scratch.
- Implement and test a small decoder-only Transformer.
- Train all model weights from random initialization using properly licensed datasets.
- Run inference locally, without network access or API keys.
- Add local chat history, model checkpoints, and optional user-controlled memory.

## Current milestone

- Android app module and XML screen.
- CMake-built C++ shared library.
- JNI calls from Java into the native library.
- Reversible byte encoding demonstration (256 byte values).
- No INTERNET permission and no external AI dependencies.

The byte demonstration is a transport/tokenization prototype only. It does not learn vocabulary merges and should not be described as BPE.

## Build

Requirements:

- Android Studio
- Android SDK Platform 35
- Android NDK (side-by-side)
- CMake 3.22.1
- JDK 17
- Android Gradle Plugin 8.6.1 / compatible Gradle 8.x

Open this repository in Android Studio, allow Gradle sync, install the SDK/NDK components requested by Android Studio, then build the `app` debug variant.

Initial ABI: `arm64-v8a`  
Minimum Android: API 27 (Android 8.1)

A Gradle wrapper is not included yet. Use Android Studio's supported Gradle setup or add a wrapper in a later build-infrastructure commit.

## Roadmap

1. Add unit tests for byte encoding/decoding and JNI boundary cases.
2. Implement a deterministic tokenizer trainer (byte-level BPE) and save/load format.
3. Implement a tiny Transformer forward pass in C++ and compare it against reference calculations.
4. Implement gradients, AdamW, gradient clipping, and numerical tests.
5. Add a small, documented, legally usable training corpus and train/validation split.
6. Add resumable checkpoints and training metrics.
7. Add autoregressive inference and connect it to the chat screen.
8. Profile memory, thermals, and speed on real Android hardware before scaling model size.

## Model integrity rules

- Initialize model parameters randomly.
- Never load or convert weights from a pretrained model.
- Record dataset sources, licenses, preprocessing, and training configuration.
- Keep training and evaluation data separate.
- Report measured results; do not claim capability based on parameter count alone.

## License

No project license has been selected yet. Until one is added, the source is not granted an open-source license by this repository.
