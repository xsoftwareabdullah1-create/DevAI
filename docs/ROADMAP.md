# DevAI technical roadmap

This roadmap deliberately separates a working Android foundation from actual model capability.

## Milestone 1 — Android/JNI foundation

- [x] Java activity and XML layout
- [x] CMake C++ shared library
- [x] JNI bridge
- [x] Reversible byte encoding demonstration
- [ ] Instrumented test on a physical ARM64 Android device
- [ ] Unicode edge-case tests (including supplementary characters)

## Milestone 2 — Tokenizer

Implement a byte-level BPE tokenizer from scratch:

1. Define reserved special tokens.
2. Count adjacent symbol pairs over a documented training corpus.
3. Iteratively merge the most frequent eligible pair.
4. Persist merge ranks and vocabulary in a versioned format.
5. Implement deterministic encode/decode.
6. Test round trips, unknown input, malformed files, and Unicode.

The current byte encoder is not BPE and does not learn merges.

## Milestone 3 — Transformer forward pass

Start with a tiny configuration suitable for numerical testing:

- decoder-only causal Transformer
- token embeddings
- causal self-attention
- RoPE positional encoding
- RMSNorm
- SwiGLU feed-forward layers
- language-model output projection

Use small deterministic tensors and compare intermediate results against independently calculated reference values.

## Milestone 4 — Training

Implement:

- next-token cross-entropy loss
- gradient computation / backpropagation
- AdamW optimizer
- gradient clipping
- deterministic random initialization
- checkpoint save/load
- training and validation metrics

Do not call a model trained until weights have actually been updated and validation is measured.

## Milestone 5 — Data

Use only datasets whose licenses and terms permit the intended use. Store source, license, retrieval date, preprocessing steps, and split metadata. Keep validation examples out of training.

## Milestone 6 — Android inference and chat

Add a generation API after the forward pass is correct. Keep inference local and remove all network dependencies. Add cancellation, bounded context, and memory-aware execution.

## Scaling policy

A 100M-parameter model is a long-term architecture target, not a realistic first on-device training run. Measure peak RAM, sustained speed, battery impact, and thermal throttling at each scale. The application must checkpoint and stop safely under memory or thermal pressure.
