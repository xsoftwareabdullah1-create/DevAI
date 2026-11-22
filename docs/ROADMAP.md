# DevAI technical roadmap

## Milestone 1 — Android/JNI foundation
- [x] Java activity and XML UI
- [x] CMake C++ shared library and JNI

## Milestone 2 — Experimental byte-level BPE tokenizer
- [x] Learn deterministic byte-pair merges from supplied text
- [x] Encode and decode token IDs
- [x] Initial host-side round-trip tests
- [ ] Persist vocabulary and merge ranks
- [ ] Harden Unicode handling and run tokenizer work off the UI thread

## Milestone 3 — Tiny decoder-only Transformer forward pass
- [x] Randomly initialized token embeddings and linear weights
- [x] RMSNorm
- [x] Causal multi-head self-attention with stable softmax
- [x] RoPE positional rotation
- [x] SwiGLU feed-forward block
- [x] Residual connections and vocabulary logits
- [x] JNI entry point and Android demo button
- [x] Native tests for finite outputs, causal prefix invariance, deterministic seeds and input bounds

This is a forward-pass reference implementation only. It does not learn from data, persist weights, or generate meaningful chat.

## Milestone 4 — Training engine
- [ ] Next-token cross-entropy loss
- [ ] Backpropagation and gradient checks
- [ ] AdamW and gradient clipping
- [ ] Mini-batch training and validation loop
- [ ] Checkpoint save/load and resume

## Milestone 5 — Dataset and evaluation
- [ ] Add licensed local dataset import and provenance records
- [ ] Deterministic train/validation split
- [ ] Report held-out loss and sample-generation quality

## Milestone 6 — Local chat and Android optimization
- [ ] Autoregressive sampling after model training
- [ ] Persist model checkpoints and tokenizer assets
- [ ] Profile arm64 CPU, memory, battery and thermals
- [ ] Optimize with NEON, quantization and bounded context where justified

## Scaling policy

Keep the initial model tiny. A 100M-parameter target is a long-term research goal, not an assumed phone capability. Increase dimensions only after measuring peak RAM, throughput and thermal behavior on target hardware.
