# Fill Option Grammar Fix

## Goal

Fix FILL question options whose distractors do not grammatically combine with the blank suffix, such as `____해` receiving nouns like `겉모습`, `센서 모양`, or `리모컨`.

## Checklist

- [x] Reproduce the reported issue from the current JSON/viewer.
- [x] Inspect FILL suffix patterns after `____`.
- [ ] Add validation that fails when a verbal blank suffix receives non-verbal-noun options.
- [ ] Update the option refinement script to select grammar-compatible FILL distractor pools.
- [ ] Regenerate JSON, seed SQL, viewer, and the 2026-05-31 bundle folder.
- [ ] Verify option quality, structural counts, seed SQL, and viewer parsing.

## Working Plan

1. Treat FILL questions with suffixes like `____해`, `____해야`, `____해요`, `____하는`, `____하는지`, or spaced `____ 해` as verbal-noun blanks.
2. Require every option in those blanks to combine naturally with the following predicate, e.g. `판단해`, `기록해`, `확인해`, `비교해`.
3. Keep answer positions and correct answer text intact; only replace incompatible distractors.
4. Preserve the existing 1,056-question / 16-mission runtime contract and regenerate derived artifacts.

## Review

- Pending.

---

# Question Bank Option Report Publishing

## Goal

Create a teammate-readable report explaining what changed in the option refinement work and publish it to the AImong Notion meeting-log database.

## Checklist

- [x] Confirm the meeting-log data source schema.
- [x] Create a local Korean markdown report.
- [x] Create the Notion meeting-log entry with the same summary, artifacts, and validation evidence.
- [x] Fetch the created Notion page to verify it exists.

## Working Plan

1. Use the existing AImong `회의록` data source (`collection://2e8bca99-39c8-8023-8551-000b9e79619c`).
2. Save the local report as `private-docs/question-bank-option-refinement-report.md`.
3. Create one new meeting-log entry with category `짧은 회의`, status `Canonical`, hub classification `구현 문서`, and date `2026-05-31`.
4. Include local artifact paths and validation commands/results in the Notion body.

## Review

- Created local report `private-docs/question-bank-option-refinement-report.md`.
- Created Notion meeting-log page `AImong 문제은행 보기 수정 보고서`.
- Notion URL: `https://www.notion.so/371bca9939c8819bbdeae5c91f3b6065`.
- Properties: `상태=Canonical`, `허브 분류=구현 문서`, `카테고리=짧은 회의`, `날짜=2026-05-31`, `기준일=2026-05-31`.
- Verification: fetched the created Notion page successfully and confirmed it lives under the AImong `회의록` data source.

---

# Question Bank Option Quality Refinement

## Goal

Revise the generated question-bank answer options so distractors reflect plausible elementary-student misconceptions, avoid obviously absurd choices, reduce repeated option sets, and preserve answer indexes.

## Checklist

- [x] Confirm the current source artifact and option-bearing question types.
- [x] Add a focused option-quality validation script that detects weak distractors, absolute-word cues, duplicate option sets, and answer-index shape errors.
- [x] Run the validation before edits to capture the current failures.
- [x] Add a scoped refinement script that rewrites only option lists while preserving questions, explanations, tags, term hints, and answer payloads.
- [x] Regenerate JSON, seed SQL, report, and static viewer from the refined question bank.
- [x] Run structural and option-quality validation after edits.

## Working Plan

1. Use `_generated/question-bank/question-bank-1056-starlevel-edits.json` as the current source of truth.
2. Treat `MULTIPLE`, `SITUATION`, and `FILL` as option-bearing question types.
3. Preserve the correct answer text first, rewrite distractors around mission-specific misconceptions, then recompute the `answer` index so it still points to the same correct text.
4. Avoid changing mission counts, pack counts, question type distribution, difficulty distribution, explanations, tags, or term hints unless the option shape requires it.
5. Rebuild derived artifacts and record validation evidence in this section.

## Review

- Source artifact: `_generated/question-bank/question-bank-1056-starlevel-edits.json`.
- Option-bearing types: `MULTIPLE` 320, `SITUATION` 320, `FILL` 208, total 848.
- Added `tasks/validate_question_bank_options.py`; before refinement it failed with 381 option-quality issues, including weak distractors, absolute-word cues, and repeated option sets.
- Added `tasks/refine_question_bank_options.py`; it preserves the correct option text at the current answer position, replaces distractors from mission-specific misconception pools, and separates phrase/example, reason, action, and fill-term option styles.
- Regenerated `_generated/question-bank/question-bank-1056-starlevel-edits.json`, `_generated/question-bank/question-bank-1056-starlevel-seed.sql`, and `_generated/question-bank/question-bank-1056-starlevel-edits-viewer.html`.
- Verification: option-quality validation passed.
- Verification: 1,056 questions, 16 missions, type distribution OX 208 / MULTIPLE 320 / FILL 208 / SITUATION 320, difficulty distribution LOW 480 / MEDIUM 320 / HIGH 256.
- Verification: answer-index errors 0, repeated option sets 0, max option set reuse 1.
- Verification: seed SQL has 1 `BEGIN`, 1 `COMMIT`, and 1,056 active question rows.
- Verification: static viewer embedded payload parsed with 1,056 questions and app JavaScript parsed with Node `vm.Script`.

---

# Question Generation Effort Documentation

## Goal

Write a teammate-facing Korean document explaining what AImong targeted in question generation and how the team invested effort, grounded in the current Notion technical docs, local question docs, generated question-bank artifacts, and validation reports.

## Checklist

- [x] Locate the relevant Notion hub/docs and local question-generation documents.
- [x] Extract the target curriculum, runtime contract, generation pipeline, validation gates, and revision/cleanup evidence.
- [x] Reconfirm current question-bank counts and key quality metrics from generated artifacts.
- [x] Draft a concise Korean document suitable for team sharing.
- [x] Publish or save the document and record verification results.

## Working Plan

1. Use the linked AImong Notion page as the document hub and cross-check local `private-docs` question-generation materials.
2. Treat the generated 1,056-question bank and validation reports as the source for concrete effort/quality claims.
3. Frame the message around: target learner/content, generation architecture, human/automated quality work, and measurable outputs.
4. Keep claims evidence-backed and avoid exaggerating unverified runtime behavior.

## Review

- Created `private-docs/aimong-question-generation-effort-summary.md` as a Korean team-sharing document.
- Created the Notion 문서 허브 page `AImong 문제 생성에 공들인 부분 정리` as a Canonical `발표 자료` document.
- Used the Notion AImong hub, `AImong 문제 생성 시스템 설계서 v1.8`, `AI 리터러시 문제 구성 개편안`, `AI 리터러시 문제은행 수정 결과 보고`, `초등학생 사용자를 고려한 AImong 문제 설계 기준`, `문제은행 편집 반영 보고서`, and `문제 생성` as source documents.
- Verification: fetched the created Notion page successfully at `https://www.notion.so/36dbca9939c881478083d2ed322ad132`.
- Verification: local UTF-8 markdown renders Korean correctly when read back.
- Verification: current JSON has 1,056 questions, 16 missions, type distribution OX 208 / MULTIPLE 320 / FILL 208 / SITUATION 320, difficulty distribution LOW 480 / MEDIUM 320 / HIGH 256, 0 required-field errors, 0 answer-index errors, 0 duplicate prompt groups, 866 questions with `termHints`, and 0 targeted banned-topic hits.
- Follow-up: strengthened the wording that questions were not invented arbitrarily, but reconstructed from KERIS, digital literacy, generative AI teaching guidance, student digital literacy research, and internal design docs.
- Follow-up: created Notion v1.1 page `AImong 문제 생성에 공들인 부분 정리 v1.1` after direct content patching on the existing page was blocked by Notion update validation; verified the new page at `https://www.notion.so/36dbca9939c881f4ad80fc8999a3e325`.

---

# Question Bank HTML Viewer

## Goal

Create a readable static HTML viewer for the revised 1,056-question AI literacy question bank.

## Checklist

- [x] Confirm the revised JSON is available.
- [x] Create a generation script for a self-contained HTML viewer.
- [x] Generate the HTML artifact under `_generated/question-bank`.
- [x] Verify counts, embedded data, filters, and static-file usability.
- [x] Document the result.

## Working Plan

1. Use `_generated/question-bank/question-bank-1056-starlevel-edits.json` as the source.
2. Generate one self-contained HTML file so teammates can open it directly without a dev server.
3. Include summary stats, mission navigation, search, stage/mission/type/difficulty filters, and readable question cards.
4. Keep the viewer read-only and avoid changing the question bank data.

## Review

- Created `tasks/generate_question_bank_html_viewer.py`.
- Generated `_generated/question-bank/question-bank-1056-starlevel-edits-viewer.html`.
- The HTML is self-contained and embeds the revised 1,056-question JSON payload, so it can be opened directly without a dev server.
- Viewer includes mission navigation, summary metrics, search, stage/mission/type/difficulty filters, answer visibility toggle, compact view toggle, readable options, answer/explanation blocks, tags, and term hints.
- Verification: embedded payload parsed successfully with 1,056 questions and 16 missions.
- Verification: embedded app JavaScript parsed successfully with Node `vm.Script`.
- Browser plugin was not available in this session, so verification was limited to static payload/script checks rather than a rendered screenshot.

---

# AI Literacy Question Bank Revision

## Goal

Revise the 1,056-question AImong question bank using the Notion meeting note and attached official AI/digital-literacy materials, while preserving the current runtime contract.

## Checklist

- [x] Fetch the Notion meeting note and extract the required curriculum direction.
- [x] Inspect the attached question-bank bundle and current generated files.
- [x] Extract targeted evidence from the attached PDF references.
- [x] Update the question bank JSON while preserving 3 stages, 16 missions, 6 packs per mission, 66 questions per mission, and existing question types.
- [x] Regenerate the seed SQL from the revised question bank.
- [x] Update curriculum/rule docs that feed generation/runtime references.
- [x] Run structural validation for counts, answer indexes, duplicate prompts, pack distribution, and reduced banned-topic leakage.
- [x] Document the result.

## Working Plan

1. Use the attached `question-bank-1056-starlevel-replaced.json` as the latest clean base because it includes `termHints` and the prior duplicate cleanup.
2. Apply the meeting-note priority changes to S0103, S0104, S0201, S0202, S0203, S0206, S0301, S0302, S0303, and S0305.
3. Keep untouched runtime contracts: mission codes, pack numbers, question IDs, type schema, difficulty bands, total counts, and answer payload shapes.
4. Re-export `_generated/question-bank/question-bank-1056-starlevel-edits.json` and `_generated/question-bank/question-bank-1056-starlevel-seed.sql`.
5. Verify mechanically before reporting completion.

## Review

- Used the Notion meeting note as the primary decision source: keep the 3-stage/16-mission/6-pack/1,056-question contract and shift content toward elementary AI literacy.
- Used the attached clean bundle as the base so prior `termHints` and duplicate-cleanup work were preserved.
- Revised targeted missions S0103, S0104, S0201, S0202, S0203, S0204, S0206, S0301, S0302, S0303, and S0305 toward data quality, recognition examples, safe AI use, privacy, source checking, bias, and fair everyday AI use.
- Aligned all 16 mission titles/summaries with the meeting-note mission map.
- Regenerated `_generated/question-bank/question-bank-1056-starlevel-edits.json`, `_generated/question-bank/question-bank-1056-starlevel-seed.sql`, and `_generated/question-bank/question-bank-1056-starlevel-edits-report.md`.
- Updated `src/main/resources/question-bank/keris-elementary-ai-16-mission-curriculum.json`, `private-docs/keris/01_stage_map.yaml`, `private-docs/keris/02_mission_rules.yaml`, and `private-docs/keris/04_gold_examples.json` to match the new AI-literacy direction.
- Verification: JSON has 1,056 questions, 16 missions, 66 questions per mission, pack distribution 10/10/10/10/10/16, type distribution OX 208 / MULTIPLE 320 / FILL 208 / SITUATION 320, difficulty distribution LOW 480 / MEDIUM 320 / HIGH 256, no answer-index errors, no duplicate prompts, and no targeted `Moral Machine` leakage.
- Verification: seed SQL has one `BEGIN`/`COMMIT`, 1,056 `question_bank` rows, and 1,056 answer-key rows.
- Verification: `.\gradlew.bat compileJava` passed after allowing Gradle to access the local cache outside the workspace.

---

# Railway Gradle Wrapper Permission Fix

## Goal

Fix Railway/Railpack deployment failure where the Linux builder cannot execute `./gradlew`.

## Checklist

- [x] Read the Railway build error and identify the failing command.
- [x] Check local Git file mode for `gradlew`.
- [x] Make the Railway build command restore executable permission before invoking Gradle.
- [x] Verify the deployment config is valid JSON and the local Gradle build still works.
- [x] Document the result.

## Review

- Root cause: Railway/Railpack failed before Gradle started because the Linux build step could not execute `./gradlew` (`Permission denied`).
- Local Git index already tracks `gradlew` as executable (`100755`), so the deployment config now defensively restores the executable bit inside the Linux builder before running Gradle.
- Updated `railway.json` build command to `chmod +x gradlew && ./gradlew bootJar -x test`.
- Verification: `railway.json` parsed successfully as JSON; `.\gradlew.bat bootJar -x test` completed with exit code 0 after allowing access to the local Gradle cache; `build/libs/backend-0.0.1-SNAPSHOT.jar` exists.

---

# Railway deployment config separation

## Goal

Keep local config behavior separate while adding a Railway-only deployment profile and paste-ready variable template.

## Checklist

- [x] Roll back global `application.yaml` Firebase JSON setting.
- [x] Add Railway-only `application-railway.yaml`.
- [x] Keep runtime support for `FIREBASE_SERVICE_ACCOUNT_JSON`.
- [x] Add a secret-free Railway variable example.
- [x] Run compile verification.

## Review

- `application.yaml` no longer declares deployment-only Firebase JSON settings.
- Added `application-railway.yaml` for Railway-only datasource, port, Firebase JSON, and pool settings.
- Added `railway.env.example` with secret-free placeholders for Railway Variables.
- Kept runtime support for `FIREBASE_SERVICE_ACCOUNT_JSON` so Railway can inject the Firebase service account without a local file path.
- Verification: `.\gradlew.bat compileJava` passed.

---

# Railway jar start command crash

## Goal

Fix Railway runtime crash caused by a start command that searches for `*/build/libs/*jar` from the wrong working directory.

## Checklist

- [x] Identify the missing jar path from Railway logs.
- [x] Add Railway config-as-code with explicit Gradle build and jar start commands.
- [x] Verify `bootJar` creates the configured jar path.

## Review

- Added `railway.json` with `./gradlew bootJar -x test` and `java -Dserver.port=$PORT -jar build/libs/backend-0.0.1-SNAPSHOT.jar`.
- Verification: `.\gradlew.bat bootJar -x test` passed and created `build/libs/backend-0.0.1-SNAPSHOT.jar`.

---

# Test Child Seed

## Goal

Create one usable test child account in the current Supabase database for login testing.

## Checklist

- [x] Confirm the child registration data model and starter resources.
- [x] Check the current database target and existing child records.
- [x] Insert a dedicated test parent, child profile, starter tickets, and streak record.
- [x] Verify the inserted code can be queried and is ready for child login testing.

## Review

- Added parent `codex-test-parent-20260516` and child `6474b526-cf00-4020-960c-d5d5c544101e`.
- Added child login code `111111`, 3 unused starter tickets, and the initial streak record.
- Verification query confirmed `profile_image_type=DEFAULT`, `total_xp=0`, `session_version=0`, `energy=20`, and the expected starter resources.

---

# Mission Schema Alignment

## Goal

Align mission-related entity mappings and the baseline schema with the current Supabase database plus canonical ERD.

## Checklist

- [x] Compare `mission_attempts`, `mission_sets`, and `mission_set_progress` against the live DB and ERD.
- [x] Add regression tests for the canonical mappings.
- [x] Update entities and submission flow to use canonical columns.
- [x] Update the baseline schema to match the canonical shapes.
- [x] Verify targeted tests and Java compilation.

## Review

- `MissionAttempt.id` now maps to `attempt_id`.
- `MissionSet` now maps the live `question_count` column instead of stale `difficulty`.
- `MissionSetProgress` now uses the canonical live shape: `mission_id`, `stage`, `first_passed_attempt_id`, `completed`, and `updated_at`; successful submit creation now populates the required mission/stage fields.
- Verification: `.\gradlew.bat test --tests com.aimong.backend.domain.mission.entity.MissionAttemptMappingTest --tests com.aimong.backend.domain.mission.entity.MissionSetMappingTest --tests com.aimong.backend.domain.mission.entity.MissionSetProgressMappingTest` passed and `.\gradlew.bat compileJava` succeeded.

---

# 테스트용 계정/토큰 기능 설계

## 목표

프론트 없이 Swagger에서 부모 API와 자녀/미션 API의 성공 케이스를 테스트할 수 있도록, 로컬 개발 환경에서만 사용할 수 있는 테스트 인증 수단을 만든다.

## 후보 방식

### 1. 실제 Firebase 테스트 계정 사용

- Firebase 테스트 계정으로 로그인해서 ID Token을 발급받는다.
- 장점: 운영 인증 흐름과 가장 비슷하다.
- 단점: 프론트나 별도 토큰 발급 도구가 필요하고, 매번 토큰 갱신이 번거롭다.

### 2. DB seed로 자녀 코드만 만들기

- 로컬 DB에 테스트 부모/자녀 데이터를 넣고, 자녀 6자리 코드로 `/child/login`을 테스트한다.
- 장점: 자녀/미션 플로우는 실제 JWT로 테스트 가능하다.
- 단점: 부모 API는 여전히 Firebase ID Token이 필요하다.

### 3. local 전용 테스트 인증 추가

- `local` 프로필에서만 동작하는 테스트 인증 방식을 추가한다.
- 예: `Authorization: Bearer local-parent-test`는 테스트 부모로 인증, `GET /dev/test-token/child`는 테스트 자녀 JWT 발급.
- 장점: Swagger에서 부모/자녀/미션 API를 모두 테스트하기 쉽다.
- 단점: 반드시 local 프로필에서만 켜지도록 안전장치가 필요하다.

## 추천안

3번 `local 전용 테스트 인증 추가`를 추천한다.

## 설계 초안

- `application-local.yaml`에 테스트 인증 설정을 둔다.
  - `aimong.dev-auth.enabled: true`
  - `aimong.dev-auth.parent-token: local-parent-test`
  - `aimong.dev-auth.parent-firebase-uid: local-parent`
  - `aimong.dev-auth.parent-email: local-parent@aimong.test`
  - `aimong.dev-auth.child-code: 111111`
- local 프로필에서만 테스트 인증이 동작한다.
- 부모 API 테스트:
  - Swagger Authorize 값: `Bearer local-parent-test`
  - `POST /parent/register` 호출 시 테스트 부모로 인증된다.
- 자녀 API 테스트:
  - `POST /parent/register`로 발급된 실제 자녀 코드를 사용하거나,
  - local seed/토큰 발급 API로 테스트 자녀 JWT를 만든다.
- 운영/production 프로필에서는 테스트 인증이 절대 동작하지 않아야 한다.

## 구현 전 확인 필요

- [ ] local 전용 테스트 인증 방식을 추가해도 되는지 확인
- [ ] 테스트 부모 토큰 값을 `local-parent-test`로 둘지 확인
- [ ] 테스트 자녀 코드를 고정값으로 둘지, 부모 등록 응답의 코드를 사용할지 결정

## Review

- 아직 구현 전 설계 단계이다.

---

# PR 기술문서/병합 리스크 리뷰

## 목표

Notion 기술문서와 대상 GitHub PR의 변경사항이 일치하는지 확인하고, `dev`에 병합할 때 충돌 가능성이 있는 파일과 의미 충돌을 점검한다.

## 체크리스트

- [x] 대상 PR 식별
- [x] Notion 기술문서 핵심 요구사항 확인
- [x] PR diff와 요구사항 대조
- [x] `dev` 기준 병합 충돌 가능성 확인
- [x] 리뷰 결과와 남은 리스크 정리

## Review

- 대상 PR은 열린 PR 중 시간순 첫 번째인 #14 `문서 기준 미션·스트릭·펫·가챠 API 구현 및 Supabase baseline 정리`로 가정했다.
- Notion 최신 정본은 기능 명세서 v2.6, API 명세서 v1.9, ERD v1.6이다.
- `git merge-tree --write-tree HEAD origin/pr-14` 기준 PR #14는 현재 `dev`에 텍스트 충돌 없이 병합 가능하다.
- PR #14 이후 #15/#16/#17과도 `git merge-tree --write-tree origin/pr-14 origin/pr-*` 기준 텍스트 충돌은 없었다.
- 다만 PR #14는 최신 문서의 단일 문항 check API를 구현하지 않았고, XP/ERD/migration 명명 등 문서 정합성 보완이 필요하다.

---

# 열린 PR별 피드백 코멘트 정리

## 목표

GitHub 열린 PR 각각에 팀원이 읽기 쉬운 피드백 코멘트 초안을 작성한다.

## 체크리스트

- [x] 열린 PR 최신 목록 확인
- [x] 각 PR의 범위와 선행 관계 확인
- [x] PR별 핵심 피드백 문구 작성
- [x] 팀원이 바로 행동할 수 있게 보완 요청과 확인 사항 정리

## Review

- 열린 PR은 #14, #15, #16, #17이다.
- #15/#16/#17은 모두 #14 head를 merge-base로 가지는 stacked PR이므로 #14를 먼저 리뷰/병합하는 흐름으로 코멘트를 작성했다.
- 코멘트는 GitHub에 직접 게시하지 않고 사용자에게 복붙 가능한 초안으로 전달한다.

---

# PR14 병합 이후 남은 PR 기술문서 정합성 리뷰

## 목표

PR #14가 `dev`에 병합된 이후 남은 PR #15/#16/#17을 최신 Notion 기술문서(API v1.9, 기능 v2.6, ERD v1.6)와 다시 대조한다.

## 체크리스트

- [x] PR #14 병합 이후 `origin/dev` 최신화
- [x] PR #15/#16/#17 diff 범위 재확인
- [x] 최신 문서 기준 핵심 요구사항 재확인
- [x] 문서와 다른 부분 및 병합 리스크 정리

## Review

- PR #15는 `origin/dev` 대비 백엔드 가챠 보완 범위로 좁아졌고, 텍스트 충돌은 없다.
- PR #15의 가챠 SR 보너스 계산은 응답값은 일반 티켓만 0보다 크게 내려가지만, 실제 확률 계산에서는 레어/에픽 티켓에도 보너스가 반영될 수 있어 문서 기준과 다르다.
- PR #16/#17은 GitHub상 mergeable=false이며, 로컬 `merge-tree` 텍스트 충돌은 없지만 multiple merge bases 경고와 큰 Android diff가 남아 있다.
- PR #16/#17에는 오프라인 퀴즈 캐시/제출 큐가 포함되어 있는데, 기능 명세서 v2.6 기준 오프라인 퀴즈 제출/캐시는 Post-MVP 범위라 현재 문서와 다르다.
- 남은 PR 어디에서도 `POST /missions/{missionId}/questions/{questionId}/check` 백엔드/앱 연동이 보이지 않아, API v1.9의 문항별 즉시 채점 흐름은 아직 미해결이다.
- PR #16의 퀘스트/업적 백엔드 구현은 `current_value`, `CHAT_GPT`, `ALL_3`, 자동/수동 claim 기준과 대체로 맞는다.
- PR #17의 챗봇 백엔드 구현은 서버 재마스킹, 일 20회, 15초 timeout, 첫 성공 XP 5, `chat_usage` 저장 기준과 대체로 맞는다.

---

# 백엔드 관련 PR 기술문서 기반 코드 리뷰

## 목표

GitHub `KDUcapstone/AImong`의 열린 PR 중 백엔드 관련 PR을 선별하고, Notion 기술문서 요구사항과 대조하여 병합 전 확인해야 할 코드 리뷰 결과를 정리한다.

## 체크리스트

- [x] GitHub 열린 PR 목록과 변경 파일 범위 확인
- [x] 백엔드 관련 PR 선별
- [x] Notion 기술문서에서 백엔드/API/ERD 요구사항 확인
- [x] PR diff를 요구사항과 대조해 결함, 누락, 회귀 위험 도출
- [x] 검증 근거와 리뷰 결과를 `tasks/todo.md` Review 섹션에 기록

## Review

- 열린 PR은 #18, #19, #20이며, #18은 Android/frontend 변경만 포함되어 백엔드 리뷰 범위에서 제외했다.
- 기준 문서는 Notion `AImong API 명세서 v1.9`, `AImong 기능 명세서 v2.6`, `AImong ERD 설계서 v1.6`, `AImong 문제 생성 시스템 설계서 v1.6`이다.
- #19는 백엔드 변경이며 `compileJava` 검증을 통과했다.
- #20은 백엔드 변경이며 패치/문서/정적 구조 기준으로 리뷰했다. Gradle 전체 검증은 PR worktree 코드가 사용자 홈 Gradle 캐시에 접근하는 외부 권한 실행으로 분류되어 안전 정책상 완료하지 못했다.
- 리뷰 결과: #19는 blocking finding 없음. #20은 부모 대시보드 서비스 테스트 부재와 대형 PR 범위가 남은 위험이며, 특히 집계 쿼리는 통합 테스트 추가 후 병합하는 것이 안전하다.

---

# Local startup DB placeholder failure

## Goal

Fix Spring Boot local startup failing because Hikari/Flyway receives the literal `${DB_URL}` while the active profiles are `supabase, local`.

## Checklist

- [x] Trace the deepest startup exception and active profile/config path.
- [x] Add local profile datasource settings that consume `LOCAL_DB_*`.
- [x] Limit the local Hikari pool for Supabase/pooler-backed local development.
- [x] Align test datasource fallback with local development config.
- [x] Verify the app context no longer receives unresolved datasource placeholders.
- [x] Document the verification result.

## Review

- Root cause: the default active profiles are `supabase, local`, but no `application-local.yaml` existed. With only `LOCAL_DB_*` configured, datasource resolution fell through to an unresolved `${DB_URL}` literal.
- Added local datasource config with `LOCAL_DB_*` fallbacks and a small local Hikari pool for Supabase/pooler-backed development.
- Verification: `.\gradlew.bat compileJava` succeeded; `.\gradlew.bat bootRun --args="--spring.main.web-application-type=none"` reached Hikari, Flyway, and JPA without the `${DB_URL}` failure; `.\gradlew.bat test --tests com.aimong.backend.BackendApplicationTests` succeeded.

---

# Safe Question Bank Editing Plan

## Notion Documentation Follow-up

- [x] Create a meeting-note entry in the AImong `회의록` database for backend teammates.
- [x] Document the edited files, original-preservation policy, change categories, content-changing answer corrections, and verification results.
- [x] Create a presentation-facing document in the AImong `문서 허브` database explaining how the question bank was designed for elementary school users.
- [x] Verify both Notion pages were created under the expected databases.

## User-Experience Follow-up Plan

- [x] Fix awkward answer-option wording that can make elementary users think the question is broken.
- [x] Reduce repeated option-set fatigue by varying repeated distractors while preserving the same correct answer.
- [x] Add short explanations for remaining English abbreviations such as `SNS` and `CCTV`.
- [x] Move overly long English proper-noun explanations out of the main question wording where possible, replacing them with shorter Korean wording.
- [x] Regenerate edited seed SQL and verify JSON structure, answer indices, duplicate prompt groups, target wording checks, and Java compilation.

## User-Experience Follow-up Review

- Fixed the awkward `learning learning AI` style option phrase and the related `learned AI` option wording that could look broken to elementary users.
- Added short in-place explanations for `SNS` and `CCTV`.
- Shortened long proper-noun activity names in question prompts, reducing the maximum question prompt length from 91 to 85 characters.
- Reduced exact same-order option-set repetition from 237 groups to 129 groups while keeping answer indices unchanged.
- Verification: edited JSON remains 1056 questions, has 0 required-field errors, 0 answer-index errors, 0 duplicate prompt groups, 0 missing `SNS`/`CCTV` explanations, and `.\gradlew.bat compileJava` succeeded.

## Latest Follow-up

- [x] Documented the backend/frontend files needed for `termHints` style concept explanations below questions, without changing the original question JSON or original seed SQL.
- [x] Rewrote 57 exact repeated prompts so the meaning stays the same but the user no longer sees the same wording; exact duplicate prompt groups within the same mission are now 0.
- [x] Added short parenthetical explanations for 13 English activity/tool names such as `Quick Draw(그림 맞히기 활동)`, `Teachable Machine(직접 학습시키는 도구)`, and `AI for Oceans(바다 생물 분류 활동)`.
- [x] Verified edited JSON parses, remains 1056 questions, has 0 required-field errors, 0 answer-index errors, 0 duplicate prompt groups, 0 remaining bare English proper nouns, and max question length is 91 characters. Edited seed SQL also has 0 remaining bare English proper nouns. `.\gradlew.bat compileJava` succeeded.

## Goal

Modify selected questions from the 1056-question generated bank while preserving the original JSON and keeping a reproducible path back to SQL seed data.

## Files

- Read-only source: `_generated/question-bank/question-bank-1056-diversified-v4-polished-current-criteria-fixed-trimmed-prompts-starlevel-high-expanded.json`
- Read-only current seed: `_generated/question-bank/question-bank-1056-starlevel-seed.sql`
- Create working copy: `_generated/question-bank/question-bank-1056-starlevel-edits.json`
- Create review diff/report: `_generated/question-bank/question-bank-1056-starlevel-edits-report.md`
- Create regenerated seed after validation: `_generated/question-bank/question-bank-1056-starlevel-edits-seed.sql`

## Checklist

- [x] Confirm the exact questions to modify by `externalId` or search text.
- [x] Copy the 1056-question JSON to a new `*-edits.json` file without changing the original.
- [x] Make only targeted edits in the working copy.
- [x] Validate total question count remains 1056.
- [x] Validate every edited question still has required fields: `externalId`, `missionCode`, `type`, `question`, `answer`, `difficulty`, `packNo`.
- [x] Validate answer indices still point to valid options for `MULTIPLE`, `FILL`, and `SITUATION`.
- [x] Generate a short report listing changed `externalId`s and before/after text.
- [x] Regenerate a new seed SQL from the edited JSON, leaving the existing seed untouched.
- [x] Run available question-bank tests or at minimum compile plus JSON validation.

## Review

- Original 1056-question JSON and original seed SQL were left untouched.
- Created `_generated/question-bank/question-bank-1056-starlevel-edits.json` as the edited working copy.
- Created `_generated/question-bank/question-bank-1056-starlevel-edits-report.md` with replacement counts and sample before/after changes.
- Created `_generated/question-bank/question-bank-1056-starlevel-edits-seed.sql` as the separate seed SQL for the edited copy.
- Wording pass replaced standalone `AI`, `데이터`, `레이블`, `프롬프트`, and `모델` in user-facing JSON text fields while preserving IDs, mission codes, answers, difficulty values, and pack numbers.
- Verification: edited JSON parses, remains 1056 questions, has 0 required-field errors, has 0 answer-index errors, and has 0 remaining target terms in user-facing fields. `.\gradlew.bat compileJava` succeeded.
- Follow-up correction: fixed Korean particle issues introduced by term replacement, including `인공지능가 -> 인공지능이`, `인공지능는 -> 인공지능은`, `인공지능와 -> 인공지능과`, and `정답 이름표링 -> 정답 이름표 붙이기`.
- Follow-up verification: edited JSON still parses, remains 1056 questions, has 0 required-field errors, has 0 answer-index errors, and has 0 remaining known awkward particle patterns in the edited JSON/report/seed SQL. `.\gradlew.bat compileJava` succeeded.
- Follow-up elementary readability correction: fixed `정답 이름표(정답 이름표)`, `인공지능 for Oceans`, `if-then`, and `학습한 인공지능이 완벽` wording in the edited JSON/report/seed SQL.
- Follow-up readability verification: edited JSON still parses, remains 1056 questions, has 0 required-field errors, has 0 answer-index errors, and has 0 remaining target readability patterns across JSON mission/question text. The edited seed SQL also has 0 remaining target readability patterns. `.\gradlew.bat compileJava` succeeded.

---

# Notion canonical spec update: termHints implementation

## Goal

Update the existing canonical feature/API specs so backend/frontend teammates can implement difficult-term helper explanations in quiz question responses.

## Checklist

- [x] Confirm target documents are the existing feature spec and API spec, not presentation docs.
- [x] Bump feature spec version and add implementation-oriented `termHints` behavior.
- [x] Bump API spec version and add response-contract summary.
- [x] Update mission/quiz API detail page with DTO, response, service, and validation notes.
- [x] Re-fetch updated Notion pages and verify versions/content.

## Review

- Updated `AImong 기능 명세서` to v3.6 with a new `termHints` behavior section under the existing quiz flow.
- Updated `AImong API 명세서` to v3.0 with a top-level response contract summary and the mission/quiz API link bumped to v2.8.
- Updated `미션 / 퀴즈 API` detail page to v2.8 with request/response contract, DTO classes, fixed-dictionary service guidance, validation rules, and version history.
- Verified all three Notion pages by re-fetching their title/version/content after edits.
