<p align="center">
  <strong>DevAI</strong><br/>
  <em>Independent offline AI research for Android</em>
</p>

<h1 align="center">DevAI · Dev Orus 1.1</h1>

<p align="center">
  <strong>Build an original language model. Run it locally. Keep control of the data and weights.</strong>
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img alt="Languages" src="https://img.shields.io/badge/Core-C%2B%2B17-00599C?logo=cplusplus&logoColor=white" />
  <img alt="UI" src="https://img.shields.io/badge/UI-Java%20%2B%20XML-orange" />
  <img alt="Mode" src="https://img.shields.io/badge/Mode-Offline%20first-276EF1" />
  <img alt="Model" src="https://img.shields.io/badge/Model-Dev%20Orus%201.1-7655C5" />
</p>

---

## 1. Project overview

**DevAI** is an independent attempt to develop an original, offline-first conversational AI for Android. Its model identity is **Dev Orus 1.1**.

The project is intended to own its complete model pipeline: tokenizer, architecture, training code, datasets, learned checkpoints, and on-device inference. The application is designed around a native Android interface written in **Java and XML**, with performance-sensitive model code written in **C++17 and exposed through JNI**.

### Independence commitment

DevAI is not intended to be a wrapper around another AI product.

- No pretrained third-party model weights.
- No Qwen, Llama, Gemma, Phi, Mistral, or other external model integration.
- No hosted AI inference API or cloud chatbot dependency.
- No teacher-model distillation or imported model checkpoints.
- Original model parameters must begin from documented random initialization.
- Training data must have recorded provenance and appropriate permission.

The project may use ordinary development libraries and build tools, but those must not supply an external AI model or hosted AI inference.

## 2. Current development status

<blockquote>
  <strong>Research prototype — not yet a trained conversational model.</strong><br/>
  The current code contains a byte-level BPE tokenizer and a tiny decoder-only Transformer forward pass. Backpropagation, an optimizer, a complete training loop, learned checkpoints, autoregressive response generation, and a real chat interface are still pending.
</blockquote>

| Component | Current status |
|---|---|
| Android Java/XML foundation | Present |
| C++17 JNI integration | Present |
| Byte-level BPE tokenizer | Experimental implementation |
| Transformer forward pass | Implemented |
| Random parameter initialization | Implemented |
| Backpropagation | Not implemented |
| AdamW optimizer | Not implemented |
| Full model training loop | Not implemented |
| Persistent learned checkpoints | Not implemented |
| Autoregressive chat generation | Not implemented |
| Chat-first interface | Planned |
| Offline conversational AI | Future milestone |

A successful forward pass is not evidence of learning. The current random model cannot understand a prompt or produce meaningful assistant responses. The existing APK, if built, must be treated as a developer prototype rather than a finished AI assistant.

## 3. Model identity and baseline architecture

**Model family:** Dev Orus  
**Version label:** 1.1  
**Project:** DevAI

The first correctness-oriented architecture baseline is intentionally small:

| Setting | Initial value |
|---|---:|
| Vocabulary size | 512 |
| Hidden dimension | 32 |
| Attention heads | 4 |
| Transformer layers | 1 |
| Feed-forward dimension | 64 |
| Context length | 64 tokens |
| Objective (planned) | Causal next-token prediction |
| Optimizer (planned) | AdamW |

This configuration is for validating implementation and training mathematics. It is not expected to deliver broad language ability. Model size, context length, and training scale should only increase after memory, speed, and quality are measured.

## 4. Technical architecture

```text
┌──────────────────────────────────────┐
│          Android application         │
│        Java + XML user interface     │
│                                      │
│  Chat  ·  Conversation history       │
│  Model status  ·  Developer Lab      │
└──────────────────┬───────────────────┘
                   │ JNI
┌──────────────────▼───────────────────┐
│           Native C++17 core          │
│                                      │
│  Tokenizer · Transformer · Training  │
│  Checkpoints · Text generation       │
└──────────────────┬───────────────────┘
                   │
┌──────────────────▼───────────────────┐
│       Dev Orus 1.1 model files       │
│  Original weights · Vocabulary       │
│  Training metadata · Local storage   │
└──────────────────────────────────────┘
```

The diagram describes the intended architecture, not a claim that every box is implemented. The current native core does not yet include training or generation.

### Android layer

- Java Activity and XML layouts.
- Native library loaded through JNI.
- Planned chat history and local model/checkpoint management.
- Long-running native work must execute away from the UI thread.
- Training and diagnostics should be placed in a separate Developer Lab instead of cluttering the normal chat screen.

### Native layer

- C++17 implementation.
- Byte-level BPE tokenizer.
- Decoder-only Transformer reference implementation.
- Planned loss, gradients, optimizer, checkpoint reader/writer, and generation loop.

## 5. Training roadmap

The training pipeline must be implemented in verifiable stages:

1. **Data preparation:** validate records, preserve language labels, deduplicate, track provenance, and create deterministic train/validation splits.
2. **Tokenization:** persist the vocabulary and merges; add special tokens for message roles and sequence boundaries.
3. **Loss:** implement numerically stable causal next-token cross-entropy.
4. **Backpropagation:** calculate and test gradients through embeddings, normalization, attention, feed-forward layers, and output projection.
5. **Optimization:** implement AdamW, gradient clipping, deterministic batching, and finite-value checks.
6. **Checkpointing:** save and reload model parameters, optimizer state, tokenizer identity, configuration, seed, and training metadata.
7. **Evaluation:** report training and held-out validation loss separately; test multilingual and multi-turn prompts.
8. **Generation:** implement autoregressive token sampling from a verified learned checkpoint.
9. **Android integration:** add chat UI, cancellation, streaming output, and lifecycle-safe native inference.

A training milestone is complete only when its tests and metrics are recorded. Merely adding examples to a JSONL file does not train the model.

## 6. Knowledge and conversation data

The repository currently has a small **198-record starter corpus**, including **60 conversation records**, across English, Hindi, and Urdu. It is intended for schema, tokenizer, and pipeline experiments—not for creating a capable general-purpose assistant.

A separate target of **200,000 conversation records** is documented as a future data goal. Those records are not currently present. The corpus must grow through reviewed, useful batches rather than repeated or fabricated filler.

Planned subject coverage includes:

- Everyday conversation, greetings, humor, and social communication.
- Language, writing, explanations, and translation.
- Mathematics, science, computing, and technology.
- History, geography, culture, and society.
- Practical problem solving and decision-making.
- Critical thinking, uncertainty, and source evaluation.
- Multi-turn dialogue, follow-up questions, corrections, and context retention.
- Creative collaboration and respectful disagreement.

Each record should be clear, useful, age-appropriate, correctly labeled, and checked for duplicates and factual reliability. Data provenance and licensing must be recorded. Validation examples must never be included in optimizer batches.

### Dataset tools

```bash
python3 scripts/validate_knowledge_dataset.py
python3 scripts/prepare_dataset.py --seed 2026
python3 -m unittest discover -s tests -p 'test_*.py'
```

## 7. Quality and evaluation principles

Dev Orus should eventually be evaluated on more than memorized facts.

- **Intent:** distinguish questions, requests, explanations, creative tasks, and casual conversation.
- **Context:** understand references to earlier turns and user corrections.
- **Language:** handle English, Hindi, and Urdu distinctly; evaluate code-switching separately.
- **Clarity:** provide useful explanations at an appropriate level of detail.
- **Reliability:** say when information is uncertain or unavailable instead of inventing facts.
- **Safety:** avoid harmful instructions and respond appropriately to sensitive situations.
- **Consistency:** compare behavior across repeated prompts and checkpoint versions.

Publish measurable results: dataset version, token count, training steps, train loss, validation loss, parameter count, elapsed time, hardware, and checkpoint hash. Do not claim universal knowledge or ChatGPT-level performance.

## 8. Continuous and resumable training

The goal is to make Dev Orus trainable in repeated, bounded sessions. Each run should resume from the latest verified checkpoint and save a new checkpoint with its metadata.

Training must not be described as running continuously when no job is active. A future scheduled workflow may run only after the training engine, data safeguards, resource limits, checkpoint recovery, and evaluation gates are implemented. No unbounded workflow loops should be used.

## 9. Android device goals

Initial Android target:

- ABI: `arm64-v8a`
- Minimum API: 27
- UI: Java + XML
- Native core: C++17 through JNI
- Operation: local/offline after model files are installed

The Redmi 9 Power is an intended test device. Actual performance, memory consumption, sustained speed, battery use, and thermal behavior must be measured on physical hardware. CI compilation alone does not prove device compatibility.

## 10. Repository structure

```text
DevAI/
├── app/
│   └── src/main/
│       ├── java/com/devai/       # Android Java entry point
│       ├── res/layout/           # XML interface
│       └── cpp/                  # C++ tokenizer, Transformer, JNI
├── data/knowledge/               # Starter JSONL dataset
├── docs/                          # Architecture, quality, and corpus plans
├── scripts/                       # Dataset validation and preparation
├── tests/                         # Native and Python tests
└── .github/workflows/             # CI and Android build workflows
```

## 11. Build environment

The current Android project configuration targets:

- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.6.1
- Android NDK
- CMake 3.22.1
- arm64-v8a

A local Android build requires the matching SDK/NDK components. Check the workflow logs and APK artifact before considering a build successful.

## 12. Roadmap

- [x] Android Java/XML project foundation.
- [x] C++17 native library and JNI bridge.
- [x] Experimental byte-level BPE tokenizer.
- [x] Tiny decoder-only Transformer forward pass.
- [x] Initial native correctness tests.
- [x] Starter multilingual knowledge/conversation corpus.
- [x] Dataset validation and deterministic split preparation.
- [ ] Persist tokenizer vocabulary and merges.
- [ ] Implement causal loss and complete backpropagation.
- [ ] Implement AdamW and gradient clipping.
- [ ] Add reproducible training and validation runs.
- [ ] Save/load versioned model and optimizer checkpoints.
- [ ] Train the first Dev Orus 1.1 checkpoint and publish real metrics.
- [ ] Add autoregressive generation.
- [ ] Build the chat-first Android interface.
- [ ] Add a separate Developer Lab and model status panel.
- [ ] Profile and test on the Redmi 9 Power.
- [ ] Expand the reviewed multilingual conversation corpus.

## 13. Transparency and limitations

DevAI is an active research project. It is not currently a trained general-purpose assistant. The first model will be small and limited; useful language ability requires more high-quality data, correct training code, sufficient compute, and repeated evaluation.

Do not interpret the model name or version label as proof of a trained release. Version 1.1 is the chosen project identity; model checkpoints and capability milestones will be versioned separately when they exist.

## 14. Contributing and development discipline

Changes should be focused, testable, and documented. For each feature:

1. State the behavior and acceptance criteria.
2. Implement the smallest coherent change.
3. Add or update tests.
4. Run relevant checks and inspect their results.
5. Record known limitations.
6. Never report an unverified build, test, or training run as successful.

## 15. License

No repository-wide license has been selected yet. Until a license file is added, this repository does not grant permission to reuse or redistribute its contents.
