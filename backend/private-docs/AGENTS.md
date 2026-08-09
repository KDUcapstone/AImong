# AGENTS.md

## Purpose
This repository serves AImong's elementary AI literacy mission system.
Always preserve the mission-based quiz contract, the 3-step curriculum flow, and the static question-bank serving rule.

## Source of truth
Use sources in this priority order:
1. `_generated/question-bank/question-bank-1056-starlevel-ultra-diverse.json`
2. `private-docs/review-v2.md`
3. `/question` PDFs
4. Feature/API/ERD specs in `private-docs`

Do not depend on raw PDF parsing, OCR, GPT generation, or runtime refill at runtime.

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

## Runtime Serving
- Serve questions only from active DB-backed question-bank rows.
- Recompose 10 questions from the mission pool by mission, child history, star level, and difficulty quota.
- In normal mode, exclude already solved questions first.
- In review mode, reuse is allowed.
- If the active pool cannot satisfy the exact 10-question quota, return `MISSION_SET_NOT_READY`.
- Do not generate replacement questions when the pool is short.

## Question Bank Rules
- Maintain the generated JSON/SQL/HTML artifacts together.
- Reject exact or near-duplicate prompts inside a selected serving set.
- Keep explanations 2 sentences or fewer.
- Do not include direct requests for real names, addresses, phone numbers, faces, voiceprints, or fingerprints.

## Question writing rules
- Write for grade 5–6 elementary students.
- Keep sentences short and concrete.
- Prefer school, homework, 발표, 사진, 목소리, 검색, 번역 앱, 분류기 활동, 친구와의 대화 contexts.
- Avoid policy or industry trend questions as direct student questions.
- Convert privacy, safety, verification, bias, and dilemma topics into daily-life situations.
- Avoid tricky wordplay.
- Keep choice lengths and tone balanced.
- Do not make the correct answer visually obvious.

## Working style
- Start with a short plan.
- Make small, reviewable changes.
- Keep external API contracts unchanged unless explicitly requested.
- Update tests and docs together with code changes.
- When changing question serving logic, also update quota and shortage tests.
