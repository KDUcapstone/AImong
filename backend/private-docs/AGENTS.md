# AGENTS.md

## Purpose
This repository serves AImong's elementary AI literacy mission system.
Always preserve the mission-based quiz contract, the 3-step curriculum flow, and the validation-first storage rule.

## Source of truth
Use sources in this priority order:
1. `🧩 AImong 문제 생성 시스템 설계서 v1.6`
2. `review-v2.md`
3. `validation-v2.txt`
4. `question-bank-v2.json`
5. `docs/keris/*.yaml`, `docs/keris/*.md`, `docs/keris/*.json`
6. Raw KERIS PDF as reference only

Do not depend on raw KERIS PDF parsing or OCR at runtime.

## Core API contract
- Question lookup is always by `missionId`.
- Client question sets must always contain exactly `10` questions.
- Public GET responses must never include answers or explanations.
- Answers and explanations are returned only after submit.
- Official question types are `OX`, `MULTIPLE`, `FILL`, `SITUATION`.
- Official content tags are `FACT`, `PRIVACY`, `PROMPT`, `SAFETY`, `VERIFICATION`.
- Keep `question_bank_safe` / public response fields separate from private answer keys.
- Never revert this system to a level-only random question selector.

## Curriculum structure
Preserve the 3-step learning flow:
- STEP 1 = concept understanding
- STEP 2 = proper use
- STEP 3 = critical verification

Stage guardrails:
- Do not inject STEP 3 source-comparison, bias, or dilemma content into STEP 1.
- Do not make STEP 2 primarily about abstract ethics debates.
- STEP 3 may include fact-checking, source comparison, bias, dual impacts, and fairness judgment.
- Use Korean suitable for grade 5–6 elementary students.
- Explanations must stay within 2 sentences.

## Seed and pool policy
The public serving contract is still 10 questions per request.
Internally, each mission must maintain a larger pool.

Required seed policy:
- total missions: `16`
- questions per mission: `60`
- packs per mission: `6`
- questions per pack: `10`
- total seed questions: `960`

Per-pack type quota:
- `OX = 2`
- `MULTIPLE = 3`
- `FILL = 2`
- `SITUATION = 3`

Per-mission type quota:
- `OX = 12`
- `MULTIPLE = 18`
- `FILL = 12`
- `SITUATION = 18`

## Difficulty policy
Use both:
- numeric difficulty: existing DB-compatible `1..4`
- difficulty band: internal quota/control metadata `LOW | MEDIUM | HIGH`

Global quota:
- `LOW : MEDIUM : HIGH = 480 : 320 : 160`

Per-mission quota:
- `LOW = 30`
- `MEDIUM = 20`
- `HIGH = 10`

Per-pack band template:
- pack 1 = `LOW 5 / MEDIUM 3 / HIGH 2`
- pack 2 = `LOW 5 / MEDIUM 3 / HIGH 2`
- pack 3 = `LOW 5 / MEDIUM 3 / HIGH 2`
- pack 4 = `LOW 5 / MEDIUM 3 / HIGH 2`
- pack 5 = `LOW 5 / MEDIUM 4 / HIGH 1`
- pack 6 = `LOW 5 / MEDIUM 4 / HIGH 1`

Important:
- `LOW`, `MEDIUM`, `HIGH` are mission-local difficulty bands.
- `HIGH` inside STEP 1 is still STEP 1 content, not STEP 3 content.
- `HIGH` inside STEP 2 is still STEP 2 content, not STEP 3 content.
- Do not satisfy quota by breaking stage rules.

Recommended numeric mapping:
- STEP 1: `LOW->1`, `MEDIUM->2`, `HIGH->2`
- STEP 2: `LOW->2`, `MEDIUM->3`, `HIGH->3`
- STEP 3: `LOW->3`, `MEDIUM->4`, `HIGH->4`

## Runtime serving and refill
- Prefer serving one intact unused pack first.
- If no intact pack is available, recompose 10 questions from the mission pool.
- In normal mode, exclude already solved questions first.
- In review mode, reuse is allowed.
- Use async refill before pool exhaustion and sync shortage refill only when needed.
- Keep mission pools near the target of `60`.

Recommended thresholds:
- `TARGET_POOL_PER_MISSION = 60`
- `SOFT_REFILL_TRIGGER = 36`
- `HARD_REFILL_TRIGGER = 18`
- `SYNC_GENERATE_BATCH = 10`
- `MINI_MAX_RETRY = 2`

## Generation rules
- Use strict structured JSON output.
- Never save generated questions before validation.
- Validate schema, type-specific shape, child safety, tags, explanation length, stage guardrails, duplicates, and pack/band quotas before insert.
- Reject near-duplicates inside the same mission.
- Reject even more aggressively inside the same pack.
- Generated explanations must be 2 sentences or fewer.
- Do not generate direct requests for real names, addresses, phone numbers, faces, voiceprints, or fingerprints.

## Question writing rules
- Write for grade 5–6 elementary students.
- Keep sentences short and concrete.
- Prefer school, homework, 발표, 사진, 목소리, 검색, 번역 앱, 분류기 활동, 친구와의 대화 contexts.
- Avoid policy or industry trend questions as direct student questions.
- Convert privacy, safety, verification, bias, and dilemma topics into daily-life situations.
- Avoid tricky wordplay.
- Keep choice lengths and tone balanced.
- Do not make the correct answer visually obvious.

## Model policy
Default model:
- `gpt-5-mini`

Escalation model:
- `gpt-5.4-mini`

Escalate when:
- mission is STEP 3
- difficulty band is `HIGH`
- numeric difficulty is `4`
- mini has failed validation 2+ times
- wording quality is weak for grade 5–6
- duplicate risk is high
- answer choices are too obvious
- explanation quality is weak

Never hardcode provider model IDs directly in business logic.
Use a routing policy abstraction and config.

## KERIS handling
- Prefer `docs/keris/01_stage_map.yaml`
- Prefer `docs/keris/02_mission_rules.yaml`
- Prefer `docs/keris/03_generation_rules.md`
- Prefer reviewed examples instead of raw PDF text
- Use raw KERIS PDF only to verify missing references or curriculum context

## Working style
- Start with a short plan.
- Make small, reviewable changes.
- Keep external API contracts unchanged unless explicitly requested.
- Update tests and docs together with code changes.
- When changing generation or refill logic, also update observability and quota tests.
