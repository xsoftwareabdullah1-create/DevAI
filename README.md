# DevAI

**An original, offline-first Android AI research project.**

**Model name: Dev Orus 1.1**

DevAI is being developed as an independently created language model. It will use its own architecture, tokenizer, training data pipeline, learned weights, and inference runtime. **No pretrained external AI model, third-party model weights, hosted AI API, or external AI service is to be integrated.**

> **Current status:** the repository currently contains an experimental byte-level tokenizer and a tiny randomly initialized C++ Transformer forward pass. It does not yet have backpropagation, an optimizer, a model training loop, or learned model checkpoints. The app is not yet a functional AI chatbot. The external-model proposal was removed before any Qwen/llama.cpp runtime or model weights were integrated.

## Non-negotiable project direction

- Build DevAI's model from random initialization; do not import pretrained weights or teacher outputs.
- Do not integrate Qwen, Llama, Gemma, Phi, Mistral, or any other external model.
- Do not call hosted AI APIs or external inference services.
- Keep the Android interface Java + XML and the native model core C++ through JNI.
- Support offline inference after the app and its own trained checkpoint are installed.
- Use only original, reviewed, or properly licensed training data with provenance recorded.
- Keep validation data separate from training and report measured results honestly.

## Current implementation

- Android Java + XML interface; C++17 core through JNI.
- Byte-level BPE tokenizer training, encode and decode.
- Decoder-only Transformer forward pass with token embeddings, RMSNorm, causal multi-head self-attention, RoPE, SwiGLU feed-forward layers, residual connections and vocabulary logits.
- Deterministic random initialization from a configurable seed.
- Input validation and a 2,000,000-parameter allocation guard.
- Original 198-record starter corpus: general knowledge, casual conversation, everyday-life topics and advanced contextual dialogues in English, Hindi and Urdu.
- Optional prior-turn context on advanced dialogue records.
- Dataset validator and deterministic train/validation split preparation.

## What must be built next

1. Implement cross-entropy next-token loss.
2. Implement backward gradients through every trainable Transformer component.
3. Implement AdamW, gradient clipping, deterministic mini-batches and validation.
4. Implement versioned model checkpoint save/load and optimizer-state handling.
5. Prove training works on a tiny synthetic task: gradients are finite and loss decreases.
6. Train on the reviewed original corpus and report train/validation loss separately.
7. Implement autoregressive generation from saved learned weights.
8. Replace the developer test screen with a chat-first Java/XML UI; move tokenizer and training controls into Developer Lab.
9. Profile inference and training on the target Android device before making performance claims.

The initial baseline remains deliberately tiny: vocabulary 512, width 32, 4 attention heads, 1 layer, SwiGLU width 64 and context length 64. This is a correctness baseline, not a promise of useful open-domain intelligence.

## Dataset status

The repository has a 198-record starter corpus, including 60 conversation records. The separate 200,000-conversation-record goal is a plan only; those records do not yet exist. Do not inflate counts with duplicate, templated, or fabricated records. A small starter set is enough to test the pipeline, not enough to create a capable general-purpose assistant.

## Training continuity and reproducibility

Training must run as explicit, reproducible jobs—not as an invisible process that is claimed to continue after a chat or workflow ends. Each completed run should save:
- model checkpoint and architecture configuration;
- tokenizer version and vocabulary size;
- dataset manifest, provenance and content hash;
- random seed, optimizer settings, step count and token count;
- training and validation loss, elapsed time and hardware details.

A future scheduled/queued training workflow may continue from the latest verified checkpoint, but only after the training loop, dataset safeguards, resource limits, checkpoint recovery and evaluation gates exist. Never start an unbounded job or report training that did not actually run.

## Build requirements

- Android SDK Platform 35
- Android NDK and CMake
- JDK 17
- Initial ABI: arm64-v8a
- Minimum Android API 27

## Important limitations

- No neural-network training loop or learned model checkpoint exists yet.
- The current Transformer weights are random and reinitialized; it cannot generate meaningful learned chat.
- Adding text records does not train the model.
- A 200,000-record corpus is a future target, not current data.
- Training a useful open-domain model requires much more high-quality data and compute than the current prototype has.
- Never claim ChatGPT-level performance or universal accuracy.

## License

No project license has been selected yet. Until one is added, this repository does not grant an open-source license.
