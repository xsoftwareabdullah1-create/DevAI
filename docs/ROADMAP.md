# DevAI technical roadmap

This roadmap distinguishes tokenizer experiments from actual learned language-model capability.

## Milestone 1 — Android/JNI foundation

- [x] Java activity and XML layout
- [x] CMake C++ shared library
- [x] JNI bridge
- [x] Reversible byte encoding prototype

## Milestone 2 — Experimental byte-level BPE tokenizer

- [x] Start from individual byte symbols (IDs 0–255)
- [x] Count adjacent symbol pairs in user-supplied corpus
- [x] Learn deterministic frequent-pair merges
- [x] Encode text using learned merges
- [x] Decode token IDs back to bytes
- [x] Add initial host-side round-trip and determinism tests
- [x] Save/load merges using a versioned binary vocabulary format
- [x] Test persistence round trips and reject malformed model files
- [ ] Test supplementary Unicode characters end-to-end through JNI
- [ ] Integrate vocabulary save/load into Android app-private storage
- [ ] Move training off the UI thread and add input-size limits

The current implementation is educational and uses JNI modified UTF-8 strings. It is not yet a production tokenizer.

Run host tests with: `sh tests/run_tokenizer_tests.sh` (requires a C++17 compiler).

## Milestone 3 — Transformer forward pass

Start with tiny deterministic tensors suitable for numerical testing:

- decoder-only causal Transformer
- token embeddings
- causal self-attention
- RoPE positional encoding
- RMSNorm
- SwiGLU feed-forward layers
- language-model output projection

Compare intermediate values against independently calculated reference results.

## Milestone 4 — Training engine

Implement and test:

- next-token cross-entropy loss
- gradient computation / backpropagation
- AdamW optimizer
- gradient clipping
- deterministic random initialization
- checkpoint save/load
- training and validation metrics

Do not call a model trained until weights have actually been updated and validation is measured.

## Milestone 5 — Data

Use only datasets whose licenses and terms permit the intended use. Record source, license, retrieval date, preprocessing, and split metadata. Keep validation examples out of training.

## Milestone 6 — Android inference and chat

Add autoregressive generation only after the forward pass is correct. Keep inference local and network-independent. Add cancellation, bounded context, and memory-aware execution.

## Scaling policy

A 100M-parameter model is a long-term architecture target, not a realistic first on-device training run. Measure peak RAM, sustained speed, battery impact, and thermal throttling at each scale. Checkpoint and stop safely under memory or thermal pressure.
