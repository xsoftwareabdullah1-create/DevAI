# DevAI: Original From-Scratch Model and Training Plan

## Project rule

DevAI must remain an independently developed model. Do not integrate pretrained external models, imported model weights, teacher logits/distillation, hosted AI APIs, or third-party inference services. In particular, do not integrate Qwen, Llama, Gemma, Phi, Mistral, or llama.cpp as an external-model runtime. The earlier proposal to use Qwen3 was a documentation-only proposal and was reverted before any such model or runtime was added to the app.

The intended stack remains Android Java/XML, C++17 through JNI, an original tokenizer, an original Transformer implementation, and DevAI's own learned checkpoints.

## Existing state

The current C++ Transformer supports a forward pass only. It has random initialization, but no backpropagation, optimizer, training loop, model checkpoint persistence, or learned language ability. The existing 198-record starter corpus includes 60 conversation records in English, Hindi and Urdu. The 200,000 conversation-record goal is not yet populated.

## First trainable baseline

- Vocabulary: 512 tokens, exactly matching tokenizer IDs.
- Width: 32.
- Attention heads: 4.
- Transformer layers: 1.
- SwiGLU width: 64.
- Context length: 64.
- Objective: causal next-token cross-entropy.
- Optimizer: AdamW with configurable learning rate and weight decay.
- Stability: gradient clipping, finite-value checks, deterministic seed.
- Batches: deterministic shuffled mini-batches; validation never enters optimizer batches.
- First execution target: host CPU reference tests and a short reproducible run; then port training/inference to Android native C++ after profiling.

This small model is only a pipeline baseline. It is not comparable to ChatGPT and will not answer arbitrary questions reliably.

## Required implementation

1. **Data preparation**
   - Validate JSONL schema and provenance.
   - Deduplicate records and related dialogue threads.
   - Group translated variants before train/validation splitting.
   - Preserve language labels; never label Urdu text as Hindi or vice versa.
   - Create role-aware sequences with explicit boundary tokens.
2. **Tokenizer**
   - Persist vocabulary and merges.
   - Verify tokenizer/model vocabulary compatibility.
   - Add deterministic encode/decode tests and Unicode round trips.
3. **Loss and gradients**
   - Implement numerically stable softmax cross-entropy.
   - Backpropagate through vocabulary head, embeddings, normalization, attention, RoPE and SwiGLU.
   - Test selected gradients against finite differences on tiny tensors.
   - Reject NaN/Inf gradients and invalid shapes.
4. **Optimizer**
   - Implement AdamW with bias correction, weight decay and gradient clipping.
   - Persist optimizer state if training is to resume exactly.
   - Make training deterministic for a fixed seed and dataset order.
5. **Checkpointing**
   - Version the checkpoint format.
   - Save all weights, architecture, tokenizer identity, optimizer state, step, seed, dataset manifest/hash and metrics.
   - Write atomically and reject incompatible/corrupt files.
   - Verify save/load produces identical logits for fixed inputs.
6. **Evaluation**
   - Report train and held-out validation loss separately.
   - Track tokens, steps, throughput, elapsed time and parameter count.
   - Test casual chat, factual explanations, follow-ups, corrections, multilingual/code-switched prompts and uncertainty.
   - Do not claim quality beyond measured results.
7. **Android app**
   - Create a chat-first Java/XML UI.
   - Put training, tokenizer and metrics in a separate Developer Lab.
   - Run native work off the UI thread with cancellation.
   - Load saved learned checkpoints; do not silently randomize weights for chat.
   - Make model state and training status visible.

## Continuous training policy

"Continuous training" means a sequence of bounded, auditable training runs that resume from the latest verified checkpoint. It does not mean an invisible process runs forever or continues after the developer session ends.

A scheduled training workflow can be introduced only after the training engine and safeguards are implemented. Each run must:
- have explicit compute/time limits;
- use a pinned dataset snapshot and seed;
- resume only from a verified checkpoint;
- preserve validation isolation;
- save checkpoint and metrics atomically;
- stop on non-finite loss, validation regression policy, or resource limits;
- publish a truthful run summary.

Do not trigger an unbounded GitHub Actions loop. GitHub Actions is for bounded CI/training experiments, not a substitute for persistent compute.

## Dataset target and truthfulness

The 200,000 conversation records are a planned target, not present in the repository. Do not fabricate records or inflate counts with repetitions. Collect and review data in small batches with documented source and permission. The current starter corpus is suitable for pipeline validation only.

## Acceptance criteria

- Native unit tests pass.
- Synthetic next-token training shows finite gradients and decreasing loss.
- A real-corpus training run completes reproducibly.
- Checkpoint reload reproduces fixed-input logits.
- Held-out validation metrics are published.
- Android APK builds and runs on the target device; performance is measured rather than assumed.

## Capability limits

A tiny model trained on a small corpus will have limited language ability. It may produce repetitive or incorrect text. Improving it requires substantially more permitted data, training compute, careful evaluation, and iterative engineering. It will not automatically match ChatGPT or know every topic.
