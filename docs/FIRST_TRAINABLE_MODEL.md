# First Trainable DevAI Model

## Purpose

Replace the current forward-pass-only demonstration with a genuinely trainable, randomly initialized language model. This is a staged engineering target, not a claim that DevAI already has ChatGPT-level ability.

## Non-negotiable model integrity

- Initialize every trainable parameter from a documented random seed.
- Do not import pretrained weights, model checkpoints, teacher logits, or external AI APIs.
- Train only on data with documented provenance and permission to use it.
- Keep training and validation examples separate; never report training loss as validation quality.
- Save the model configuration, tokenizer version, seed, optimizer settings, dataset manifest/hash, step count, and metrics beside every checkpoint.
- Report limitations and measured results; never call a random forward pass a trained model.

## First baseline target

Use the existing decoder-only Transformer as the architecture starting point, but add the missing training system:

- Vocabulary: 512 tokens initially (must match the tokenizer/model vocabulary exactly).
- Width: 32.
- Attention heads: 4.
- Transformer layers: 1.
- SwiGLU width: 64.
- Context length: 64 tokens initially.
- Approximate parameter budget: about 43,000 parameters for the current untied-embedding layout.
- Objective: next-token cross-entropy.
- Optimizer: AdamW with configurable learning rate, weight decay, gradient clipping, and deterministic seed.
- Training: shuffled mini-batches, padding/masking where required, periodic validation, and early-stop/checkpoint controls.
- Runtime: first validate a reproducible host-side CPU training run; then port the same math and checkpoint format to the Android C++ runtime. Do not assume a phone can sustain a long training run until measured.

This is deliberately a tiny research baseline. It is not comparable to ChatGPT in knowledge, reasoning, or reliability. A small parameter count does not imply useful conversation ability.

## Required implementation sequence

1. **Data contract**
   - Create a conversation-only training manifest from reviewed JSONL records.
   - Preserve language labels (English, Hindi, Urdu, and Hinglish only when explicitly labeled).
   - Deduplicate exact and near-duplicate records.
   - Group translated variants and related dialogue threads before splitting.
   - Keep validation records completely out of optimizer batches.
   - Record provenance and licensing for every added source.
2. **Tokenizer and sequence preparation**
   - Persist and reload the exact tokenizer vocabulary/merges.
   - Verify that every token ID is within the configured model vocabulary.
   - Add BOS/EOS and conversation-role boundaries with a documented format.
   - Create deterministic packed sequences and train/validation splits.
3. **Training math**
   - Implement cross-entropy, backward gradients through all trainable layers, AdamW, gradient clipping, and optimizer-state handling.
   - Add finite-value checks and gradient tests.
   - Prove that a tiny synthetic next-token task overfits and that loss decreases.
4. **Checkpointing**
   - Save/load all model weights and required configuration atomically.
   - Reject incompatible vocabulary, architecture, or checkpoint versions.
   - Test exact output reproducibility after save/reload.
5. **Evaluation**
   - Report train and validation loss separately, token count, steps, elapsed time, and parameter count.
   - Add held-out prompt tests for greetings, casual chat, factual explanations, follow-ups, corrections, Hindi/English code-switching, and uncertainty.
   - Do not describe fluent or factual conversation ability until these tests support it.
6. **Android chat integration**
   - Replace the current developer-test landing screen with a chat-first Java/XML interface.
   - Keep training controls and metrics in a separate Developer Lab.
   - Load a trained checkpoint instead of randomizing weights on each JNI call.
   - Generate autoregressively with bounded context and cancellation; keep the UI responsive.
   - Clearly display when no trained checkpoint exists.

## Dataset reality at plan creation

The repository currently has a 198-record starter corpus, including 60 conversation records. The separate 200,000-conversation-record goal is a plan only; those 200,000 records do not yet exist. The starter set is useful for pipeline tests, not sufficient for training a useful open-domain assistant. Do not pad it with duplicated, templated, or fabricated records to make the count look larger.

## Acceptance gates

A milestone is complete only when all relevant evidence is available:

- Native and host tests pass.
- A synthetic task demonstrates gradients and decreasing loss.
- A reproducible real-corpus training run completes and writes a checkpoint.
- Reloaded checkpoint produces identical logits for a fixed input.
- Held-out validation metrics are reported.
- Android build succeeds and runtime is checked on the target device before claiming device compatibility.

## What this does not promise

The first model will be a small experimental language model. It will not match ChatGPT, know every topic, or reliably answer arbitrary questions. Meaningful capability depends on architecture, data quality and scale, training compute, and evaluation—not just adding a large number of text records.
