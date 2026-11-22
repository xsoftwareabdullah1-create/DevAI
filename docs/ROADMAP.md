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

## Milestone 4 — Beginner knowledge and conversation seed corpus
- [x] Add original general-knowledge Q&A in English, Hindi and Urdu (150+ knowledge and conversation records)
- [x] Add friendly casual conversation examples: greetings, thanks, jokes, riddles, boredom and playful nonsense
- [x] Add detailed everyday-life and discussion examples: communication, disagreements, digital footprint, misinformation, AI limitations, habits, money basics and complex questions
- [x] Add response-style metadata for detailed answers
- [x] Add 24 advanced multi-turn dialogue records across English, Hindi and Urdu, with explicit prior-turn context
- [x] Cover causal reasoning, systems thinking, perspective-taking, decision-making, uncertainty, creative collaboration, conflict repair and source evaluation
- [x] Define UTF-8 JSONL record format with IDs, language, level, tags and provenance
- [x] Add a Python standard-library dataset validator
- [x] Add deterministic train/validation split preparation with concept-group isolation
- [x] Add unit tests for split reproducibility, record preservation, duplicate IDs and Unicode
- [x] Run dataset validation in native CI
- [ ] Expand to additional languages in reviewed, correctly labeled batches
- [ ] Expand beginner facts, then intermediate and advanced curriculum levels
- [ ] Add dialogue-turn-aware training examples and evaluate context retention across held-out conversations
- [ ] Build the 200,000-record conversation corpus in reviewed batches: 66,667 English, 66,667 Hindi and 66,666 Urdu
- [ ] Add reviewed sources and license metadata for future third-party datasets

The current corpus contains 198 original records (174 earlier records plus 24 advanced contextual dialogue records; 66 records per language). The separate 200,000-record conversation target is a planned expansion, not an existing dataset. See [CONVERSATION_CORPUS_PLAN.md](CONVERSATION_CORPUS_PLAN.md) for allocation and quality gates. They do not change model weights until a training pipeline exists. Casual prompts should be handled politely and playfully when appropriate, not labeled as stupid; unclear prompts should invite clarification.

## Milestone 5 — Response quality and evaluation contract
- [x] Define all-round response quality standard across conversation, knowledge, coding, reasoning, creativity and everyday topics
- [x] Define answer calibration, context-use, language-quality and detail-selection rubric
- [ ] Build a held-out multilingual evaluation suite and reviewer scoring workflow
- [ ] Add automated factuality, duplicate and multi-turn regression checks where measurable

## Milestone 6 — Training engine
- [ ] Next-token cross-entropy loss
- [ ] Backpropagation and gradient checks
- [ ] AdamW and gradient clipping
- [ ] Mini-batch training and validation loop
- [ ] Checkpoint save/load and resume

## Milestone 7 — Dataset pipeline and evaluation
- [ ] Add scalable licensed local dataset import and provenance records
- [x] Deterministic train/validation split (preparation utility only; training loop still pending)
- [ ] Curriculum sampler: beginner → intermediate → advanced
- [ ] Per-language evaluation and held-out loss reporting

## Milestone 8 — Local chat and Android optimization
- [ ] Autoregressive sampling after model training
- [ ] Persist model checkpoints and tokenizer assets
- [ ] Profile arm64 CPU, memory, battery and thermals
- [ ] Optimize with NEON, quantization and bounded context where justified

## Scaling policy

Keep the initial model tiny. A 100M-parameter target is a long-term research goal, not an assumed phone capability. Increase dimensions only after measuring peak RAM, throughput and thermal behavior on target hardware.
