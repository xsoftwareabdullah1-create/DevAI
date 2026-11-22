# DevAI conversation corpus scale target

## Target

Build a reviewed corpus of **200,000 conversation records**, separate from knowledge/fact records. This is a target, not the current dataset size. The current starter corpus contains 198 total records; only 60 are conversation records.

Proposed language allocation:

| Language | Target records |
|---|---:|
| English (en) | 66,667 |
| Hindi (hi) | 66,667 |
| Urdu (ur) | 66,666 |
| **Total** | **200,000** |

The allocation can be revised after language-quality evaluation. Do not count a translated copy as an independent dialogue unless it is reviewed for natural phrasing and cultural/contextual fit.

## Conversation coverage

Build balanced batches across:
- casual chat, greetings, humor and harmless playful prompts;
- everyday life, hobbies, friendships, school/work situations and practical decisions;
- explanations, open-ended discussion and follow-up questions;
- multi-turn context retention and references to earlier turns;
- reasoning, comparisons, cause and effect, trade-offs and uncertainty;
- creative collaboration, brainstorming and constructive feedback;
- respectful disagreement, misunderstandings and conflict repair;
- technical, scientific, historical, social and philosophical discussions at multiple levels;
- asking clarifying questions when the request is ambiguous;
- concise answers when requested and detailed answers when useful.

## Record quality requirements

Every record must:
1. Have a stable unique ID, language label, topic tags and provenance.
2. Contain a meaningful user/assistant exchange; multi-turn examples must include correctly ordered context.
3. Be natural in its labeled language. Hindi and Urdu must not be machine-translated English with only vocabulary substitutions.
4. Avoid duplicates and low-value template substitutions. Exact duplicate checks are mandatory; semantic near-duplicate review should be added before large-scale ingestion.
5. Be fact-checked where it makes factual claims, and express uncertainty where evidence is incomplete.
6. Be reviewed for age-appropriate, respectful and safe responses.
7. Record source and license metadata for any third-party material. Do not ingest copyrighted books, websites or datasets without permission.

## Staged delivery

Do not generate 200,000 low-quality placeholder rows in one batch. Build and validate batches, initially 1,000–5,000 records, and expand only after measuring:
- schema validity and unique IDs;
- language balance and native-language review;
- exact and semantic duplicate rates;
- topic and difficulty coverage;
- multi-turn context integrity;
- held-out conversation quality.

Keep train/validation/test groups isolated by conversation family and translated concept to prevent leakage. Report actual accepted record counts after each batch.

## Model limitation

A larger corpus does not itself improve DevAI. The current Transformer is a randomly initialized forward-pass prototype; backpropagation, optimizer, training loop, checkpointing and context-aware training are still pending. The 200,000-record target must not be described as completed until the records exist, pass validation and are checked into the repository or a documented dataset artifact.
