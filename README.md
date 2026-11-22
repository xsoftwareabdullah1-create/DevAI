<p align="center">
  <img src="https://raw.githubusercontent.com/xsoftwareabdullah1-create/DevAI/main/assets/branding/devai-logo.svg" alt="DevAI logo" width="150" />
</p>

<h1 align="center">DevAI · Dev Orus 1.1</h1>
<p align="center"><strong>Private, on-device AI chat for Android.</strong></p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img alt="UI" src="https://img.shields.io/badge/UI-Java%20%2B%20XML-orange" />
  <img alt="Native" src="https://img.shields.io/badge/Native-C%2B%2B17-00599C?logo=cplusplus" />
  <img alt="Inference" src="https://img.shields.io/badge/Inference-Offline%20after%20download-276EF1" />
</p>

## What is Dev Orus 1.1?

DevAI is the Android application and **Dev Orus 1.1** is the assistant identity shown in the chat experience. This release uses the complete **Google Gemma 4 E2B instruction-tuned model** for language generation, executed locally with Google's LiteRT-LM runtime.

This is an integration release, not a claim that the Gemma weights were independently trained by this repository. The underlying model remains Gemma 4 E2B and is attributed to Google in this documentation and model information. The assistant-facing product identity is Dev Orus 1.1.

## Current release · 0.4.0

- ChatGPT-style Java + XML chat interface: slide-in sidebar, centered start screen with suggestions, rounded message bubbles, pill-shaped composer, and copy-on-long-press for messages.
- Settings screen with the Gemma model download, progress, cancel, and delete controls.
- C++17 JNI bridge to the official LiteRT-LM C API.
- Stateful local conversation using Gemma 4 E2B.
- Model download from **Settings** with progress display, storage check, cancel, retry, and delete. The chat screen shows a banner until the model is installed and loaded.
- Model weights are stored in app-private storage and are not bundled into the APK.
- After the one-time download, text generation runs on-device without a hosted AI API.
- Existing tokenizer, experimental Transformer, and starter-corpus research code remain in the repository, but are not the active chat inference engine in this release.

## First launch

1. Install the arm64 APK.
2. Open the sidebar (☰) → **Settings**, tap **Download Gemma model**, and confirm the approximately 2.6 GB download. Wi-Fi is recommended.
3. Keep at least 3.2 GB of free storage available during installation.
4. Wait for the model to initialize. The chat becomes available when the status says the model is ready.
5. After setup, you can disable internet access and continue chatting locally.

If the download is interrupted, the incomplete temporary file is discarded and the download can be retried from Settings. If model initialization fails, use the **Retry** button on the chat screen to load the installed model again.

## Model and runtime attribution

| Item | Details |
|---|---|
| Assistant identity | Dev Orus 1.1 |
| Underlying model | Google Gemma 4 E2B IT |
| Model format | LiteRT-LM `.litertlm` |
| Model package | About 2.59 GB |
| Inference runtime | Google LiteRT-LM C API 1.0.0 |
| Runtime release | LiteRT-LM v0.19.1 |
| Native integration | C++17 through JNI |
| Android UI | Java + XML |
| ABI | arm64-v8a |
| Minimum Android | API 27 (Android 8.1) |
| Inference backend | CPU |

Model source: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm

Runtime source and releases: https://github.com/google-ai-edge/LiteRT-LM

The model repository identifies the LiteRT Community Gemma 4 E2B IT package as Apache-2.0. Review the model repository's current license and the Google model terms before redistribution or commercial use. LiteRT-LM is distributed under its own Apache-2.0 license.

## Build from source

The project uses Android Gradle Plugin 8.6.1, Gradle 8.7, Java 17, Android SDK 35, CMake 3.22.1, and NDK 27.2.12479018 in CI.

The official LiteRT-LM C API Android runtime is downloaded during the build and its release archive is SHA-256 verified. It is not committed as a large binary into this repository.

```bash
bash scripts/prepare_litertlm_android.sh
gradle --no-daemon :app:assembleDebug
```

The SDK preparation script expects `curl`, `unzip`, and `sha256sum`. The APK build targets arm64-v8a only.

## Repository layout

```text
DevAI/
├── app/src/main/java/com/devai/       # Chat + Settings activities, ModelManager (download)
├── app/src/main/res/                  # XML layouts, drawables, colors, styles
├── app/src/main/cpp/                  # JNI bridge, tokenizer, Transformer
├── scripts/prepare_litertlm_android.sh # Verified native runtime setup
└── .github/workflows/                 # Android APK and native test workflows
```

## Important limitations

- The first model download is large and requires a stable connection and sufficient free storage.
- Model initialization and generation speed depend on the phone's CPU, RAM, thermal limits, and Android build. CI compilation does not prove performance on every device.
- This release configures the CPU backend for broad device compatibility. GPU/NPU acceleration is not enabled in this integration.
- The current chat is text-first. You can attach any file type (+ button): text-based files and the text files inside `.zip` archives are read and passed to the model (capped at about 8,000 characters in total because of the on-device context window). Binary formats such as images, PDFs, Office files, or non-zip archives are attached by name only; the model cannot read their contents. Document parsing, image understanding, and file creation are not included.
- The app does not train Gemma weights on the phone. The active language-generation engine uses the downloaded pretrained Gemma model.
- No hosted inference API or paid API key is required.

## Project integrity

The product name Dev Orus 1.1 does not change the identity or provenance of the underlying Gemma model. Model attribution is retained here so users can distinguish the assistant experience from the model provider. Do not describe this release as an independently trained original Dev Orus neural model.

---
