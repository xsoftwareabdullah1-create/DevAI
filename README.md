# DevAI

**Offline-first Android AI chat application.**

DevAI is moving from its experimental scratch-built Transformer demo to a practical on-device assistant using an existing open-weight model. The app will retain a native Android Java/XML interface and use C++ through JNI for local inference.

> **Current status:** the repository still contains the earlier tokenizer and tiny random-weight Transformer demo. The Qwen3 model and llama.cpp runtime are the selected replacement architecture, but they have not yet been integrated or tested in the APK. Do not treat the current APK as a working AI chat app.

## Selected model and runtime

- **Model:** [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B), an instruction/chat-capable open-weight model published under Apache 2.0.
- **Android model format:** GGUF, initially Q4_K_M quantization (approximately 0.48 GB; exact file and SHA-256 must be pinned before shipping).
- **Inference runtime:** [llama.cpp](https://github.com/ggml-org/llama.cpp), built for Android arm64-v8a and called from Java through JNI.
- **Target device:** Redmi 9 Power class hardware, beginning with CPU inference and a short context window. Actual speed and memory use must be measured on the phone.
- **Offline behavior:** download the model file once, store it in app-private storage, then perform inference locally without an AI API or network connection.

The upstream Qwen3 model card identifies the model as Apache-2.0. The GGUF is a converted/quantized representation, so the final release must retain upstream model attribution, conversion provenance, exact source revision, file hash, and applicable notices.

## App experience

- Chat-first home screen with message bubbles, composer, send/stop controls, and streaming response display.
- Conversation history stored locally.
- Model manager with download progress, file verification, storage estimate, and delete/re-download controls.
- Settings for context length, response length, CPU thread count, and generation temperature, with safe device-aware defaults.
- Developer Lab separated from normal chat: runtime status, model metadata, token speed, load time, memory estimate, and diagnostic logs.
- Clear error states when the model is missing, incompatible, or cannot fit in available memory.

## Implementation plan

1. Pin a compatible llama.cpp revision and Qwen3 GGUF file; record SHA-256 and licensing notices.
2. Build llama.cpp for Android arm64-v8a using the Android NDK and CMake.
3. Add a JNI wrapper for model loading, token generation, cancellation, and cleanup.
4. Implement the Java/XML chat UI and background inference execution.
5. Add verified model download/import and private-storage management.
6. Test prompt formatting, multi-turn context, Hindi/English/Hinglish behavior, cancellation, app lifecycle, and low-memory handling.
7. Build the APK and test on the actual Redmi 9 Power before claiming device compatibility.

## Important limitations

- A 0.6B model is small and will make mistakes; it is not comparable to ChatGPT and cannot guarantee correct answers to every question.
- Q4 quantization reduces storage and memory use but can reduce output quality.
- Model download requires internet once; chat inference after the model is stored is offline.
- Model file size is not the same as total runtime RAM. Context cache, Android, and other apps consume additional memory.
- Device speed, thermals, and battery use are unknown until measured on target hardware.
- No fine-tuning is included in the first integration. Any later fine-tuning must use reviewed, licensed data and be evaluated separately.

## Previous research prototype

The repository's earlier C++17 byte-level BPE tokenizer and randomly initialized tiny Transformer are retained for educational/reference purposes only. They are not the production chat engine and do not provide learned language ability. The old 198-record seed corpus and 200,000-conversation planning document are research assets, not a trained model dataset.

## Build requirements

- Android SDK Platform 35
- Android NDK and CMake
- JDK 17
- Initial ABI: arm64-v8a
- Minimum Android API 27

## License

The DevAI application code has not yet selected a project license. The integrated Qwen3 model has its own upstream Apache-2.0 license and attribution requirements; do not imply that model weights are authored by DevAI.
