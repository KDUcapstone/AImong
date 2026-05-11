# 00_curriculum_summary.md

## Purpose
This document is the shortest human-readable entry point for AImong's KERIS-based curriculum layer.
Use it before reading `01_stage_map.yaml`, `02_mission_rules.yaml`, or `03_generation_rules.md`.

This file explains:
- what parts of the KERIS material are actually used,
- how the 3-step AImong structure was derived,
- which chapter groups map to which student-facing missions,
- how to keep question generation aligned with elementary AI literacy rather than drifting into teacher-facing policy or theory text.

---

## Source basis
Operational source priority for curriculum interpretation:
1. `🧩 AImong 문제 생성 시스템 설계서 v1.4`
2. `review-v2.md`
3. `validation-v2.txt`
4. `question-bank-v2.json`
5. `docs/keris/01_stage_map.yaml`
6. `docs/keris/02_mission_rules.yaml`
7. raw KERIS PDF as reference only

This means the raw PDF is important, but it is **not** the first thing runtime generation should depend on.
For generation, prefer reviewed summaries and structured rules.

---

## Trust note
KERIS 1권 is the core reference used for this system, but it is not identical to a final nationwide fixed standard.
The book itself notes that its contents reflect the authors' views and may differ in part from later curriculum/content standards.

Therefore, AImong should treat this curriculum layer as:
- **KERIS-based and classroom-usable**,
- **operationally stable for elementary AI literacy**,
- but still **a structured teaching reference**, not a legal or policy document.

---

## What KERIS contributes to AImong
The KERIS 1권 material contributes three things that matter directly for this project.

### 1. School-level direction
For elementary school, the book frames AI education as:
- 놀이·체험 중심으로 AI 소양 습득
- AI의 이해
- AI의 원리 적용
- 사회적 영향(윤리 포함)

AImong translates this into a student-facing 3-step progression instead of asking children policy-heavy or teacher-facing questions.

### 2. Integrated AI education model
The book explains AI education through three overlapping types:
- 이해교육
- 활용교육
- 가치교육

It also argues these should be applied in an integrated way rather than as isolated blocks.
AImong operationalizes this as:
- STEP 1 = understanding,
- STEP 2 = use,
- STEP 3 = critical/value-aware verification.

### 3. Concrete classroom activity anchors
Chapter 3 gives elementary-friendly activity anchors that are much more generation-friendly than raw policy pages.
Important anchors include:
- 우리 생활 속 인공지능
- 언플러그드 지도학습 / 비지도학습
- 언플러그드 강화학습
- AI for Oceans
- Teachable Machine

These anchors are more useful for question generation than national trend summaries.

---

## Target learner interpretation
AImong should interpret this curriculum for:
- **Korean elementary grade 5–6 learners first**
- classroom, homework, presentation, and simple digital-tool contexts

This is important because many KERIS classroom activity pages in Chapter 3 are explicitly written for upper elementary use.
So the writing level should be:
- short and concrete,
- conceptually accurate,
- but not low-elementary simplified,
- and not middle-school abstract.

---

## What parts of KERIS should drive student questions
Use these chapter groups as the **primary curriculum engine**.

### Primary student-question source group A — concept understanding
Use mainly:
- Chapter 2.1 인공지능의 개념과 발전 과정
- Chapter 2.2 규칙 기반 vs. 학습 기반 인공지능
- Chapter 2.3 딥러닝의 이해
- Chapter 3.1 우리 생활 속 인공지능
- Chapter 3.2 언플러그드 활동: 지도학습 및 비지도학습
- Chapter 3.4 AI for Oceans

This group mainly feeds STEP 1 and part of STEP 2.

### Primary student-question source group B — correct and safe use
Use mainly:
- Chapter 3.1 우리 생활 속 인공지능
- Chapter 3.4 AI for Oceans
- Chapter 3.5 Teachable Machine
- Chapter 4.2 인공지능의 편향성

This group mainly feeds STEP 2.

### Primary student-question source group C — verification, bias, ethics, impact
Use mainly:
- Chapter 4.1 인공지능의 양면성
- Chapter 4.2 인공지능의 편향성
- Chapter 4.3 인공지능의 딜레마
- Chapter 4.4 인공지능 데이터 편향성 체험 활동

This group mainly feeds STEP 3.

---

## What parts of KERIS should mostly stay background-only
The following areas are useful for teacher understanding, but should **not** become direct student question sources unless carefully rewritten.

### Background-only by default
- Chapter 1.1 인공지능 정책·산업 동향
- Chapter 1.2 인공지능 교육 동향
- long country-by-country policy comparisons
- industry trend tables
- teacher-facing framework comparisons
- dense theory lists from AI4K12 or ethics frameworks when copied too literally

These areas can inform the system's worldview, but direct student items such as:
- “Which country launched what plan?”
- “What year was which strategy announced?”
- “What is the policy title?”
should be avoided.

---

## AImong 3-step translation
AImong reinterprets KERIS as a clean 3-step learner journey.

### STEP 1 — AI가 뭐예요?
**Role:** concept understanding  
**Goal:** AI의 원리와 한계 이해

This step introduces:
- 생활 속 AI 사례
- AI와 계산기의 차이
- 데이터, 레이블, 학습, 테스트의 기초
- 딥러닝과 인식의 기초
- AI도 틀릴 수 있다는 점

This step should feel like:
- concept introduction,
- misconception correction,
- short, clear judgments,
- observation and comparison of simple examples.

This step should **not** feel like:
- source comparison drills,
- bias theory lecture,
- ethical dilemma debate,
- policy or philosophy class.

### STEP 2 — AI 잘 쓰기
**Role:** proper use  
**Goal:** 프롬프트 작성과 개인정보 보호

This step introduces:
- 목적이 보이는 질문
- 조건이 구체적인 질문
- 개인정보와 생체정보 보호
- 사진·음성·데이터 수집의 기본 원칙
- AI 결과를 보고 고치기
- AI 도움을 받고 내 말로 정리하기

This step should feel like:
- classroom practice,
- safe digital behavior,
- revision and retry,
- choosing better prompts,
- collecting data more carefully.

This step should **not** turn into:
- national policy interpretation,
- abstract ethics lecture,
- adult security compliance content.

### STEP 3 — 비판적 사고
**Role:** critical verification  
**Goal:** 팩트체크와 출처 확인, 편향과 양면성 판단

This step introduces:
- 핵심 주장 찾기
- 출처·날짜·기관·근거 비교
- 대표성 부족과 데이터 편향
- 기술의 양면성
- 공정성과 딜레마

This step should feel like:
- checking,
- comparing,
- asking why,
- judging fairness in daily-life language,
- noticing that AI answers need evidence.

This step should **not** become:
- college-level ethics terminology drills,
- legal document memorization,
- abstract philosophy debate.

---

## Mission structure summary
Base curriculum structure:
- total stages: `3`
- total missions: `16`
- base question count: `160`
- mission-based serving size: `10 questions`

Expanded seed structure used by AImong generation:
- questions per mission: `60`
- packs per mission: `6`
- questions per pack: `10`
- total expanded seed: `960`

Stage-level structure:
- STEP 1: `5 missions / 50 base / 300 expanded`
- STEP 2: `6 missions / 60 base / 360 expanded`
- STEP 3: `5 missions / 50 base / 300 expanded`

---

## Difficulty interpretation
AImong uses two difficulty views together.

### Numeric difficulty
For DB compatibility:
- `1`
- `2`
- `3`
- `4`

### Mission-local difficulty band
For exact quota and pack planning:
- `LOW`
- `MEDIUM`
- `HIGH`

Important rule:
`LOW / MEDIUM / HIGH` are **local to the mission and stage**.
They do not permit importing higher-stage concepts.

Examples:
- STEP 1 HIGH is still STEP 1 content.
- STEP 2 HIGH is still STEP 2 content.
- STEP 3 LOW is still STEP 3 verification content.

---

## Official question formats and tags
Official question types:
- `OX`
- `MULTIPLE`
- `FILL`
- `SITUATION`

Official content tags:
- `FACT`
- `PRIVACY`
- `PROMPT`
- `SAFETY`
- `VERIFICATION`

These are not optional style hints.
They are part of the operational contract shared by question generation, validation, storage, and serving.

---

## Service interpretation of the curriculum
This curriculum is not served directly by stage.
It is served by `missionId`.

That means:
- `stage` expresses educational hierarchy,
- `missionId` expresses runtime lookup and delivery,
- `packNo` and `difficultyBand` express internal pool control,
- the client still receives exactly `10` questions.

So the curriculum must always be translated as:
- **pedagogy by stage**,
- **delivery by mission**,
- **validation by schema and rules**.

---

## Practical generation boundaries
When generating questions, keep these boundaries.

### Prefer
- daily-life school situations
- short explanation of why
- safe use habits
- comparing examples
- checking evidence
- fixing prompts
- refining data collection
- AI as a helper, not as a replacement for thinking

### Avoid
- policy titles and dates
- government program memorization
- teacher-only evaluation language
- long copied theory sentences
- abstract ethical jargon without concrete context
- real personal identifiers in prompts or options

---

## Why the 3-step structure is a strong fit
This structure works well because it matches both:
1. the KERIS direction of **understanding + use + social impact**, and
2. the design needs of AImong's mission-based question serving.

In practice, the structure does four useful jobs at once:
- it keeps STEP 1 simple and confidence-building,
- it gives STEP 2 concrete safe-use skills,
- it lets STEP 3 introduce verification and fairness without becoming too abstract,
- it keeps the curriculum stable enough to scale from 160 reviewed items to a 960-question seeded pool.

---

## Recommended reading order inside docs/keris
1. `00_curriculum_summary.md` — shortest overview
2. `01_stage_map.yaml` — stage, counts, packs, quotas
3. `02_mission_rules.yaml` — mission-by-mission rules
4. `03_generation_rules.md` — generation and validation instructions
5. `04_gold_examples.json` — compact reference examples

---

## One-line operational summary
AImong's KERIS curriculum layer should be read as:
**“upper-elementary AI literacy, organized as concept understanding → proper use → critical verification, delivered by missionId, and generated from structured curriculum rules rather than raw PDF text.”**
