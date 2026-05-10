# 03_generation_rules.md

## Purpose
This document defines the canonical generation rules for AImong's KERIS-based elementary AI literacy question system.
It is optimized for Codex, server-side runtime generation, and human review.

Always preserve three things first:
1. the `missionId`-based serving contract,
2. the 3-step curriculum flow,
3. the validation-first storage rule.

---

## Source of truth
Use sources in this priority order:
1. `🧩 AImong 문제 생성 시스템 설계서 v1.4`
2. `review-v2.md`
3. `validation-v2.txt`
4. `question-bank-v2.json`
5. `docs/keris/01_stage_map.yaml`
6. `docs/keris/02_mission_rules.yaml`
7. raw KERIS PDF as reference only

Do not depend on raw KERIS PDF parsing or OCR at runtime.
Do not treat policy or industry trend pages as direct student question sources.

---

## Core service contract
- Question lookup is always by `missionId`.
- Client-facing question sets must always contain exactly `10` questions.
- Public GET responses must never include answers or explanations.
- Answers and explanations are returned only after submit.
- Official question types are `OX`, `MULTIPLE`, `FILL`, `SITUATION`.
- Official content tags are `FACT`, `PRIVACY`, `PROMPT`, `SAFETY`, `VERIFICATION`.
- `prompt` is the DB field name, but API responses use `question`.
- Generated questions must never be saved before validation succeeds.

---

## Curriculum frame
Preserve the 3-step learning flow exactly.

### STEP 1 — AI가 뭐예요?
Role: concept understanding  
Goal: AI의 원리와 한계 이해

Generate questions about:
- 생활 속 AI 사례
- AI와 계산기의 차이
- 데이터, 레이블, 학습, 테스트의 기초
- 딥러닝과 인식의 기초
- AI의 한계와 다시 확인하기

Do not generate STEP 1 questions that mainly require:
- 심화 출처 비교
- 사회구조 수준 편향 분석
- 딜레마 심화 토론
- 법/정책 원문 이해

### STEP 2 — AI 잘 쓰기
Role: proper use  
Goal: 프롬프트 작성과 개인정보 보호

Generate questions about:
- 좋은 질문 만들기
- 목적과 조건 추가
- 개인정보와 생체정보 보호
- 데이터 다양성, 허락, 저작권
- 테스트, 수정, 재질문
- AI 도움을 받고 내 답으로 정리하기

Do not generate STEP 2 questions that mainly require:
- 국가 AI 정책 해석
- 산업 동향 설명
- 추상 윤리 논쟁 중심 판단
- 고난도 편향 이론 설명

### STEP 3 — 비판적 사고
Role: critical verification  
Goal: 팩트체크와 출처 확인, 편향과 양면성 판단

Generate questions about:
- 팩트체크
- 출처, 날짜, 기관, 근거 비교
- 대표성 부족과 데이터 편향
- 기술의 긍정/부정 영향 함께 보기
- 공정성, 딜레마, 선택의 어려움

Do not generate STEP 3 questions as:
- 대학 수준 윤리학 용어 암기
- 법/정책 원문 암기형
- 전문 철학 토론형
- 성인 정책 보고서 요약형

---

## Student language rules
Target audience:
- Korean grade 5–6 elementary students

Language rules:
- Keep sentences short.
- Use concrete daily-life language.
- Avoid teacher-facing meta explanations.
- Avoid abstract jargon unless a short plain explanation is included.
- Keep explanations within 2 sentences.
- Use calm, non-scolding tone.
- Do not use fear-heavy privacy examples.
- Do not request real names, real addresses, real phone numbers, real faces, real voiceprints, or real fingerprints.

Preferred contexts:
- 학교
- 숙제
- 발표
- 번역 앱
- 카메라 앱
- 검색
- 그림 분류
- 사진 수집
- 목소리
- 친구와의 대화
- AI for Oceans
- Teachable Machine
- Quick Draw

Avoid direct question themes:
- 국가 AI 정책 자체를 묻는 문제
- 산업 동향 암기형 문제
- 성인 대상 법/보안 정책 원문
- 대학 수준 이론 설명 문제

---

## Seed and pool rules
The public serving contract remains `10 questions per request`.
Internally, each mission maintains a `60-question` pool.

### Initial seed target
- total missions: `16`
- questions per mission: `60`
- packs per mission: `6`
- questions per pack: `10`
- total seed questions: `960`

### Per-pack type quota
- `OX = 2`
- `MULTIPLE = 3`
- `FILL = 2`
- `SITUATION = 3`

### Per-mission type quota
- `OX = 12`
- `MULTIPLE = 18`
- `FILL = 12`
- `SITUATION = 18`

Do not break these quotas unless explicitly requested by a human maintainer.

---

## Difficulty policy
Use two parallel fields:
- numeric difficulty: DB-compatible `1..4`
- difficulty band: internal control field `LOW | MEDIUM | HIGH`

### Band quota per mission
- `LOW = 30`
- `MEDIUM = 20`
- `HIGH = 10`

### Band template per pack
- pack 1 = `LOW 5 / MEDIUM 3 / HIGH 2`
- pack 2 = `LOW 5 / MEDIUM 3 / HIGH 2`
- pack 3 = `LOW 5 / MEDIUM 3 / HIGH 2`
- pack 4 = `LOW 5 / MEDIUM 3 / HIGH 2`
- pack 5 = `LOW 5 / MEDIUM 4 / HIGH 1`
- pack 6 = `LOW 5 / MEDIUM 4 / HIGH 1`

### Important interpretation rule
`LOW`, `MEDIUM`, and `HIGH` are **mission-local** difficulty bands.
They are not permission to import higher-stage concepts.

Examples:
- STEP 1 `HIGH` is still STEP 1 content.
- STEP 2 `HIGH` is still STEP 2 content.
- STEP 3 `LOW` is still STEP 3 verification content.

### Numeric difficulty mapping
- STEP 1: `LOW -> 1`, `MEDIUM -> 2`, `HIGH -> 2`
- STEP 2: `LOW -> 2`, `MEDIUM -> 3`, `HIGH -> 3`
- STEP 3: `LOW -> 3`, `MEDIUM -> 4`, `HIGH -> 4`

### How to make a question harder without changing stage
Increase difficulty by:
- requiring slightly more comparison,
- reducing overt clues,
- improving distractor quality,
- asking for better judgment in a daily-life situation,
- requiring one more verification step.

Do **not** increase difficulty by:
- importing older-student concepts,
- using legalistic language,
- using policy jargon,
- forcing philosophical abstraction.

---

## Official question types
Only these four types are allowed.

### OX
Use for:
- quick concept checks
- clear safety habits
- common misconceptions

Rules:
- `options = null`
- `answer` must be boolean
- wording must be short and direct
- avoid double negatives

### MULTIPLE
Use for:
- best action / best explanation / best example choice
- concept comparison
- one-correct-answer judgment

Rules:
- exactly 4 options
- only 1 correct answer
- distractors must be plausible
- option length and tone should be balanced
- avoid one obviously longer “correct-looking” choice

### FILL
Use for:
- simple concept reinforcement
- keyword completion
- guided vocabulary in context

Rules:
- no free-text answers
- options length = 4–5
- `answer` is an index array
- choices should be single words or short phrase cards
- blank sentence must still sound natural to grade 5–6 students

### SITUATION
Use for:
- daily-life decision making
- privacy/safety/verification habits
- applied judgment

Rules:
- short everyday scenario only
- options length = 2–4
- exactly 1 correct answer
- the correct answer must reflect the safest / most reasonable / most evidence-based action

---

## Content tag rules
Only these tags are allowed:
- `FACT`
- `PRIVACY`
- `PROMPT`
- `SAFETY`
- `VERIFICATION`

Guidance:
- `FACT`: concept accuracy, core meaning, concrete understanding
- `PRIVACY`: personal information, biometric information, consent, non-identifying alternatives
- `PROMPT`: asking better questions, adding purpose/conditions, revising prompts
- `SAFETY`: responsible use, caution, copying avoidance, safe participation
- `VERIFICATION`: fact-checking, comparing sources, re-checking claims, checking labels/data quality

Tag rules:
- use only mission-appropriate tags
- prefer 1–2 tags, sometimes 3 if clearly justified
- do not attach tags mechanically
- do not create new tag names

---

## Prompt construction rules for generators
When generating a question candidate, always provide:
- `missionCode`
- `stage`
- `missionTitle`
- `packNo`
- `difficultyBand`
- `numeric difficulty`
- `preferredQuestionType`
- `allowedConcepts`
- `bannedConcepts`
- `preferredContentTags`
- `curriculumRef`
- target audience = grade 5–6

Always instruct the generator to:
- output strict JSON only,
- keep explanation within 2 sentences,
- avoid duplication with existing mission questions,
- stay inside the mission’s allowed concepts,
- keep the scenario grounded in daily student life,
- avoid direct requests for real personal data.

---

## Canonical output shape
Use strict JSON only.

```json
{
  "missionCode": "S0203",
  "packNo": 1,
  "difficultyBand": "LOW",
  "type": "MULTIPLE",
  "question": "string",
  "options": ["string", "string", "string", "string"],
  "answer": 0,
  "explanation": "string",
  "contentTags": ["PRIVACY", "SAFETY"],
  "curriculumRef": "string",
  "difficulty": 2
}
```

Validation assumptions:
- `missionCode` is required
- `packNo` is required
- `difficultyBand` is required
- `type`, `question`, `explanation` are required
- `contentTags` must be from the official set only
- `difficulty` must follow the stage mapping

---

## Quality rules
### What good questions look like
- The answer is clear after thinking, not after guessing a weird wording trick.
- The wrong choices are believable but wrong for a reason.
- The scenario feels like something a student could actually face.
- The explanation says **why**, not just repeats the answer.
- The question teaches a habit or concept, not only a quiz trick.

### What bad questions look like
- It depends on specialized adult knowledge.
- It uses social-policy or industry-analysis language.
- The correct answer is much longer or much more moral-sounding than others.
- It exposes or requests sensitive personal data.
- It is essentially the same as an existing question with words swapped.

---

## Validation-first storage rule
Never save generated questions before validation.

Minimum validation chain:
1. parse strict JSON
2. validate official type
3. validate type-specific shape
4. validate child safety
5. validate tag set
6. validate explanation length
7. validate mission/stage guardrails
8. validate pack/type/band quota fit
9. validate duplicate / near-duplicate risk
10. save only on full success

If any step fails, reject the candidate.

---

## Duplicate and similarity rules
Reject questions that are too close to existing questions in the same mission.
Reject even more aggressively inside the same pack.

Check at least two levels:
1. normalized string / token / n-gram duplication
2. semantic near-duplicate similarity

Common duplicate failures:
- same scenario skeleton with changed nouns only
- same explanation template reused too often
- same distractor pattern repeated pack after pack
- same correct action wrapped in slightly different wording

Preferred diversity dimensions:
- different daily-life contexts
- different wrong-choice patterns
- different verbs and sentence frames
- different comparison angles
- different safety/verification triggers

---

## Runtime refill rules
- Prefer intact unused pack serving first.
- If no intact unused pack exists, recompose from the mission pool.
- In normal mode, exclude solved questions first.
- In review mode, reuse is allowed.
- Async refill should happen before exhaustion.
- Sync shortage refill should remain a small fallback path.

Recommended thresholds:
- `TARGET_POOL_PER_MISSION = 60`
- `SOFT_REFILL_TRIGGER = 36`
- `HARD_REFILL_TRIGGER = 18`
- `SYNC_GENERATE_BATCH = 10`
- `MINI_MAX_RETRY = 2`

---

## Model routing rules
Default model:
- `gpt-5-mini`

Escalation model:
- `gpt-5.4-mini`

Escalate when:
- mission belongs to STEP 3
- difficulty band is `HIGH`
- numeric difficulty is `4`
- mini has failed validation at least 2 times
- duplicate risk is high
- wording quality is weak for grade 5–6
- options are too obvious
- explanation quality is weak

Never hardcode provider model IDs directly in business logic.
Always route through config and a routing policy abstraction.

---

## Human review checklist
Before approving a new batch, reviewers should quickly check:
- Is the stage correct?
- Is the mission focus correct?
- Is the language grade 5–6 friendly?
- Does the question avoid adult policy language?
- Is the explanation short and useful?
- Is there any privacy risk?
- Is the question meaningfully distinct from neighboring questions?
- Does the pack still satisfy type and difficulty quotas?

---

## Final rule
When there is a conflict, prefer in this order:
1. mission safety and child appropriateness
2. mission/stage curriculum fit
3. service contract correctness
4. quota balance
5. stylistic variety

If a question is high-quality but breaks the stage contract, reject it.
If a question is on-stage but unsafe, reject it.
If a question is safe and on-stage but duplicates an existing one, reject it.
