# Termux: Run Dev Orus with a local language model

Dev Orus can use either of two backends:

- \`retrieval\`: the project's own corpus-based TF-IDF matcher. It selects stored answers.
- \`llama-server\`: generates responses using a model file stored on this device through llama.cpp.

The second backend uses an external, pre-trained open-weight model. It is optional and does not replace the project's original retrieval system or make the original Transformer trained.

## Suggested starter model

- Model: \`Qwen/Qwen2.5-0.5B-Instruct-GGUF\`
- Quantization: \`Q4_K_M\`
- Approximate model file size: 491 MB
- Model license: Apache 2.0
- Runtime: llama.cpp
- Network: required only to download source/build files and the model; inference uses the local model file.

The model card describes multilingual support. Hindi and Urdu quality can vary at this small size, so evaluate both with your own prompts rather than assuming perfect support. Check the model card and license before redistribution.

## 1. Build llama.cpp in Termux

Use a stable Termux installation and ensure enough free storage and battery. Building C++ code can take time and heat the phone.

\`\`\`bash
pkg update
pkg install git cmake clang make libandroid-spawn python -y

cd ~
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp

cmake -B build -DGGML_OPENMP=OFF -DGGML_NATIVE=OFF
cmake --build build --config Release -j2 --target llama-server
\`\`\`

If the build fails, keep the complete error output; do not assume the binary was created.

## 2. Download the model once

This is the only model download step. The model is not stored in the DevAI Git repository.

\`\`\`bash
python -m pip install -U huggingface_hub
mkdir -p ~/models/qwen
hf download Qwen/Qwen2.5-0.5B-Instruct-GGUF qwen2.5-0.5b-instruct-q4_k_m.gguf --local-dir ~/models/qwen
\`\`\`

If the Hugging Face CLI reports that the filename does not exist, open the model's Files tab and choose the exact \`Q4_K_M\` filename shown there.

## 3. Start the local model server

Keep this Termux session running:

\`\`\`bash
cd ~/llama.cpp
./build/bin/llama-server \
  -m ~/models/qwen/qwen2.5-0.5b-instruct-q4_k_m.gguf \
  --host 127.0.0.1 --port 8080 \
  -c 2048 -t 4
\`\`\`

The server is bound to loopback (\`127.0.0.1\`), not exposed to your local network. Once the model is loaded, inference does not need internet.

## 4. Start Dev Orus in a second Termux session

\`\`\`bash
cd ~/DevAI
git pull
python scripts/devorus_cli.py --backend llama-server
\`\`\`

One-shot test:

\`\`\`bash
python scripts/devorus_cli.py --backend llama-server --ask "Explain Python functions in simple Hindi."
\`\`\`

The CLI sends requests only to \`http://127.0.0.1:8080\`. It does not call a hosted AI provider.

## 5. Compare with the original retrieval backend

\`\`\`bash
python scripts/devorus_cli.py --backend retrieval
\`\`\`

The two modes are deliberately separate. The local Qwen model generates answers; the retrieval backend returns stored corpus answers.

## 6. Test suite

\`\`\`bash
PYTHONPATH=scripts python -m unittest discover -s tests -p 'test_*.py' -v
\`\`\`

## Troubleshooting

- \`Cannot reach the local llama-server\`: make sure the first Termux session is still running and the server finished loading.
- \`No such file\`: verify the downloaded GGUF filename with \`ls -lh ~/models/qwen\`.
- Out-of-memory or Android killing the process: close other apps, use a smaller context such as \`-c 1024\`, and avoid running the build and model at the same time.
- Slow responses: CPU-only inference is expected to be slower than cloud services. This small model is a starter, not a frontier model.
- Language quality: evaluate Hindi and Urdu separately; a model's multilingual label does not guarantee equal quality in every language.

## Privacy and cost

There is no API key, subscription, or hosted inference call in this setup. The downloaded model file and llama.cpp runtime stay on the device. Initial downloads and updates require internet and data; after setup, chatting can be offline.
