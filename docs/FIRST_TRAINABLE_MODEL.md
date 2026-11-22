# DevAI On-Device Model Integration

## Decision

Use an existing open-weight instruction model instead of trying to train a useful language model from random initialization on a phone.

## Model choice

- Upstream: Qwen/Qwen3-0.6B
- Upstream license: Apache-2.0 (verify the exact model card and include its notices in the app/release).
- Initial deployment format: GGUF Q4_K_M.
- Expected model file size: approximately 0.48 GB for the referenced conversion; exact artifact, source revision, and SHA-256 must be pinned before release.
- Runtime: llama.cpp.
- Android target: arm64-v8a, CPU first, short initial context (e.g. 2048 tokens) to control memory.

The Qwen3 model is a pretrained model, not a DevAI-created or scratch-trained model. Its use is an intentional change from the earlier no-pretrained-weights experiment.

## Why this choice

The existing project has only a forward-pass-only Transformer with random weights and no backpropagation, optimizer, or checkpoint training loop. Integrating a compact instruction model is a much more realistic route to an actual offline chat experience than pretending the tiny prototype has learned language.

## Android architecture

```
Java/XML Chat UI
    |
    | JNI calls
    v
C++ DevAI bridge
    |
    v
llama.cpp inference engine
    |
    v
Qwen3-0.6B GGUF (app-private storage)
```

- Keep the interface in Java/XML; do not migrate the app to Kotlin.
- Build llama.cpp with Android NDK for arm64-v8a.
- Use a background executor/thread for generation; never run inference on the UI thread.
- Stream generated text to the UI in small chunks.
- Support stop/cancel and release model/context resources when appropriate.
- Store chat history locally; construct the correct Qwen3 chat template for each turn.
- Download or import the model once, verify SHA-256, then allow offline inference.
- Do not bundle a roughly half-gigabyte model inside the APK; ship it as a separate model download/import.

## Model download and integrity

Before enabling a model download in a release build:
1. Select the exact GGUF repository and filename.
2. Pin an immutable upstream revision.
3. Record exact byte size and SHA-256.
4. Verify the file hash after download before loading.
5. Include original model attribution, Apache-2.0 license text, and conversion attribution where required.
6. Never silently substitute a different quantization or file.

## Memory and device constraints

A 0.48 GB file does not mean 0.48 GB total RAM usage. Runtime memory includes model tensors, context/KV cache, inference buffers, Android, and other running apps. Start with CPU inference and a 2048-token context; measure actual peak memory, generation speed, temperature, and battery on the Redmi 9 Power. If it struggles, reduce context and concurrency before considering a smaller quantization.

## Acceptance tests

- Model file downloads/imports and SHA-256 verification rejects corrupted files.
- llama.cpp loads the pinned Qwen3 GGUF on arm64 Android.
- Basic English, Hindi, and Hinglish prompts produce non-empty generated text.
- Multi-turn chat includes prior messages within the configured context.
- Stop button cancels generation without freezing the UI.
- Model unload and app lifecycle do not leak native resources.
- No network calls occur during inference after the model is installed.
- Measure and report load time, tokens/second, peak memory, and thermal behavior on the actual target phone.
- Build and install the APK; do not claim physical-device success based only on CI.

## Fine-tuning later

The first goal is to run the pretrained model correctly. Fine-tuning is a separate later milestone and requires:
- A reviewed, properly licensed dataset.
- A supported training method and compatible checkpoint format.
- Suitable compute outside a mid-range phone if necessary.
- Held-out evaluation to check whether fine-tuning improves the desired behaviors.
- Clear attribution that the base model is Qwen3 and DevAI's fine-tuning data/adapters are separate.

## Honest limitations

Qwen3-0.6B is a small model. It can answer many ordinary prompts, but it will still hallucinate, misunderstand some language, and perform poorly on harder reasoning tasks. It is not ChatGPT-level. Offline use begins only after the model file is downloaded or imported.
