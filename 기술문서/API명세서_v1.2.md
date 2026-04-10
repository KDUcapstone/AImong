# 📡 AImong API 명세서 v1.2

생성자: 한욱
카테고리: 기술 문서
최종 업데이트 시간: 2026년 4월 5일

> **버전**: v1.2 | **수정일**: 2026-04-05 | **기반**: API 명세서 v1.1 + 교차검증 보고서 반영

> 🤖 **이 문서는 바이브코딩(Cursor, Copilot 등) 친화적으로 작성되었습니다.**

> 각 API의 요청/응답/예외/구현 노트를 복붙해서 바로 구현할 수 있도록 상세하게 명세합니다.

---

# 🆕 v1.2 핵심 변경 요약

> 💡 **팀원에게**: v1.1 대비 무엇이 바뀌었는지 아래 표로 먼저 확인하세요. 구현 시작 전 반드시 읽어주세요!

| # | 변경 항목 | 한 줄 요약 | 영향 담당 |
| --- | --- | --- | --- |
| 1 | DELETE /streak/partner 구현 노트 수정 | `child_a_id/child_b_id` → ERD 대칭 2행 모델(`child_id`) 기준으로 정정 | BE |
| 2 | GET /pet 응답에 `mood` 필드 추가 | FE가 펫 감정 상태를 API로 받도록 명시 | BE/FE |
| 3 | PUT /pet/equip 신규 추가 | 펫 장착 변경 API. MVP 이후 2번째 펫부터 필요 | BE/FE |
| 4 | GET /missions/daily 신규 추가 | 오늘의 AI 미션 조회 전용 API | BE/FE |
| 5 | GET /streak/milestones 신규 추가 | 사용자 목표 마일스톤 조회 | BE/FE |
| 6 | POST /streak/milestone 신규 추가 | 사용자 목표 마일스톤 설정 (30일 이후 25일 단위) | BE/FE |
| 7 | POST /streak/milestone/claim 신규 추가 | 목표 마일스톤 보상 수령 | BE/FE |
| 8 | GET /parent/children 신규 추가 | 부모가 자신의 자녀 목록 조회 | BE/FE |
| 9 | DELETE /parent/child/{childId} 신규 추가 | 자녀 프로필 삭제 (CASCADE) | BE/FE |
| 10 | POST /parent/fcm-token 신규 추가 | 부모 FCM 토큰 등록/갱신 | BE/FE |
| 11 | POST /privacy/event detectedType에 `ETC` 추가 | ERD ENUM과 일치하도록 보완 | BE |
| 12 | POST /missions/{missionId}/submit `idempotency_key` 명시 | 오프라인 재전송 중복 방지 — 헤더로 전달 | BE/FE |
| 13 | 복습 XP의 펫 XP 적립 규칙 확정 | 복습 시 펫 XP도 적립하지 않음 (공통 XP 함수 수정) | BE |
| 14 | "다음 등급 알 해금" 정책 폐기 확정 | `newEggUnlocked` 필드 삭제는 v1.2에서 정식 확정 | BE/FE |

---

# 🆕 v1.1 핵심 변경 요약

> (v1.1 변경 내용은 하위 호환성을 위해 유지)

| # | 변경 항목 | 한 줄 요약 | 영향 담당 |
| --- | --- | --- | --- |
| 1 | 세션 무효화 방식 변경 | Redis 블랙리스트 → `sessionVersion` DB 컬럼 방식 | BE |
| 2 | `quizAttemptId` 도입 | 문제 조회~제출을 하나의 세션으로 묶어 정합성 보장 | BE |
| 3 | 문제 수 부족 시 500 반환 통일 | 부분 반환(4문제 등) 금지, 반드시 5문항 또는 500 | BE |
| 4 | XP 캐시 날짜 경계 초기화 규칙 명시 | 날짜/주차 바뀌면 today_xp, weekly_xp 0으로 초기화 | BE |
| 5 | 퀘스트 보상 AUTO/MANUAL 구분 | MISSION_1·CHAT_GPT는 자동지급(AUTO), 나머지는 수동 | BE/FE |
| 6 | 복귀 보상 중복 지급 방지 로직 개선 | "오늘 수령" → "같은 결석 구간 수령" 기준으로 변경 | BE |
| 7 | 동시성 제어 규칙 명시 | 가챠/퀘스트/파트너 등 6개 API에 FOR UPDATE 트랜잭션 | BE |
| 8 | 부모 통계 집계 기준 명시 | is_review=false인 최초 완료만 카운트 | BE |
| 9 | `/weak-points` 설명 수정 | 문제 유형 분석 → 미션 단위 분석으로 정정 | - |
| 10 | 경로 변수명 통일 | `{id}` → `{childId}`, `{missionId}`로 전부 통일 | BE/FE |

---

# 🌐 공통 규칙

## Base URL

```
https://api.aimong.app/api
```

## 인증 방식

| 대상 | 방식 | 헤더 |
| --- | --- | --- |
| 부모 (PARENT) | Firebase Auth ID Token | `Authorization: Bearer {firebase_id_token}` |
| 자녀 (CHILD) | 서버 발급 세션 토큰 | `Authorization: Bearer {child_session_token}` |

> 모든 인증 필요 API는 토큰 없으면 401 반환. Spring Boot `OncePerRequestFilter`로 처리 권장.

## 공통 응답 형식

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": { "code": "ERROR_CODE", "message": "사람이 읽을 수 있는 설명" } }
```

## 공통 에러 코드

| HTTP | code | 설명 | 발생 상황 |
| --- | --- | --- | --- |
| 400 | BAD_REQUEST | 요청 파라미터 오류 | 필수 필드 누락, 잘못된 값 |
| 401 | UNAUTHORIZED | 인증 실패 | 토큰 없음/만료/유효하지 않음 |
| 403 | FORBIDDEN | 권한 없음 | 다른 자녀 데이터 접근 시도 |
| 404 | NOT_FOUND | 리소스 없음 | 존재하지 않는 ID |
| 409 | CONFLICT | 중복 요청 | 이미 완료된 작업 재시도 |
| 429 | TOO_MANY_REQUESTS | 한도 초과 | GPT 20회, 코드 로그인 3회 실패 |
| 500 | INTERNAL_ERROR | 서버 오류 | 예기치 못한 서버 에러 |
| 504 | GATEWAY_TIMEOUT | 외부 API 타임아웃 | GPT 15초 초과 |

## ⚠️ 전역 규칙 (구현 시 반드시 준수)

```
1. XP 반올림: 모든 XP 계산 결과에 Math.floor 적용 (Math.round 절대 금지)
2. 타임존: 모든 날짜 계산 KST(UTC+9) 기준. JVM 타임존 Asia/Seoul 설정 필수
3. 정답 비공개: GET /missions/{missionId}/questions 응답에 answer, explanation 절대 미포함
4. 가챠 난수: 반드시 서버 사이드 생성 (클라이언트 조작 방지)
5. 채점: 서버에서만 처리, 클라이언트에 정답 데이터 전송 금지
6. 원문 텍스트: 개인정보 원문은 서버 DB 저장 금지 (감지 유형만 저장)
```

## v1.1 추가 전역 규칙

> 💡 **BE(한일)에게**: 아래 규칙은 v1.0에 없던 신규 규칙입니다.

**① 날짜/시간 직렬화 규칙**

| 필드 성격 | 형식 | 예시 필드 |
| --- | --- | --- |
| LocalDate (날짜만) | `yyyy-MM-dd` KST 기준 | `date`, `completedAt`, `weekStart`, `lastCompletedDate` |
| Instant (시각) | ISO-8601 UTC | `obtainedAt`, `detectedAt`, `lastActiveAt` |

**② 경로 변수명 통일**

- 자녀 경로: `{childId}` / 미션 경로: `{missionId}` — `{id}` 표기 사용 금지

**③ 동시성 제어 대상 API** — 반드시 DB 트랜잭션 + row lock 처리

```
- POST /missions/{missionId}/submit
- PUT /parent/child/{childId}/regenerate-code
- POST /gacha/pull
- POST /gacha/exchange
- POST /streak/partner
- POST /streak/milestone
- POST /streak/milestone/claim
- POST /quests/claim
- POST /return-reward/claim
- PUT /pet/equip
```

**④ XP 캐시 초기화 규칙**

- `today_xp_date != today(KST)` → `today_xp = 0` 초기화 후 적립
- `weekly_xp_week_start != 이번 주 월요일(KST)` → `weekly_xp = 0` 초기화 후 적립

**⑤ 미션 완료 통계 집계 기준**

- `missionCount` 계열 통계는 `mission_progress.is_review = false`만 집계
- 복습(is_review=true)은 카운트하지 않음

**⑥ shieldCount 규칙**

- v1.1에서는 획득/조회만 정의. 스트릭 유지 로직에서 자동 소비하지 않음 (v1.3 예정)

## 🆕 v1.2 추가 전역 규칙

**⑦ 오프라인 재전송 idempotency_key 전달 방식**

- 미션 제출 시 `Idempotency-Key` **요청 헤더**로 전달 (UUID v4 권장)
- 헤더가 없으면 서버에서 UUID를 생성하여 기록 (재전송 보호 없음 상태)
- 헤더가 있으면 동일 key 재요청 시 이전 응답 그대로 반환

**⑧ 복습 시 펫 XP 비적립 확정**

- 복습 모드(`isReview = true`)일 때는 **펫 XP도 적립하지 않음**
- child.totalXp / todayXp / weeklyXp도 복습 XP(절반)만큼 적립
- 단, 펫 XP는 0 — 이하 공통 XP 적립 함수에 반영됨

**⑨ "다음 등급 알 해금" 정책 폐기 확정**

- 아이몽 달성 시 `newEggUnlocked` 필드는 응답에서 완전 제거
- 기능명세서 v2.1의 "다음 등급 알 해금" 설명도 폐기 예정 (기능명세서 v2.2에서 반영)
- 현재 정책: 아이몽 달성 시 pet.xp 리셋 + 왕관/티켓 보상만 지급

## 공통 보상 응답 모델 (v1.1 신규)

> 💡 퀘스트 수령, 미션 제출, 스트릭 마일스톤 등 여러 곳에서 `rewards[]` 배열로 통일됩니다.

```json
// 티켓 보상 예시
{
  "type": "TICKET",
  "ticketType": "NORMAL",
  "count": 1,
  "reason": "DAILY_QUEST_XP_20"
}

// XP 보상 예시
{
  "type": "XP",
  "amount": 5,
  "reason": "DAILY_QUEST_CHAT_GPT"
}
```

## 공통 XP 적립 함수 규칙 (v1.2 수정)

> 모든 XP 보상은 아래 순서로 처리

```
1. Math.floor 적용
2. today_xp, weekly_xp 경계 초기화 후 적립
3. child_profiles.total_xp += amount
4. child_profiles.today_xp += amount
5. child_profiles.weekly_xp += amount
6. ⚠️ isReview = false일 때만: 장착 펫이 있으면 pets.xp += amount  ← v1.2 수정
   (복습 모드에서는 펫 XP 비적립)
7. 업적 체크 수행
8. 퀘스트/업적 후속 갱신 수행
```

---

# 🔐 인증 API

## POST /parent/register

> 부모 온보딩 — Google 로그인 후 자녀 프로필 생성, 6자리 코드 발급, 스타터 티켓 3장 지급

**담당**: BE (문한일) | **인증**: PARENT (Firebase ID Token)

**Request Body**

```json
{ "nickname": "민준" }
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| nickname | string | ✅ | 1~20자, 공백만으로 구성 불가 |

**Response 201**

```json
{
  "success": true,
  "data": {
    "childId": "3f1a2b4c-...",
    "nickname": "민준",
    "code": "482917",
    "starterTickets": 3
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| Firebase 토큰 유효하지 않음 | 401 | UNAUTHORIZED | 유효하지 않은 토큰입니다 |
| nickname 누락 또는 빈 문자열 | 400 | BAD_REQUEST | 닉네임을 입력해주세요 |
| nickname 20자 초과 | 400 | BAD_REQUEST | 닉네임은 20자 이하여야 합니다 |
| 코드 생성 충돌 5회 초과 | 500 | INTERNAL_ERROR | 코드 생성에 실패했습니다. 다시 시도해주세요 |

**구현 노트**

```
1. Firebase Admin SDK로 ID Token 검증 → parent_accounts 조회 또는 신규 INSERT
2. 6자리 랜덤 숫자 코드 생성 → child_profiles.code UNIQUE 충돌 시 최대 5회 재시도
3. 트랜잭션 시작
   - child_profiles INSERT (starter_issued = false)
   - tickets INSERT (normal = 3)
   - child_profiles.starter_issued = true
   - streak_records INSERT (child_id)
4. 트랜잭션 커밋
```

---

## POST /child/login

> 자녀 6자리 코드로 로그인 — 세션 토큰 발급

**담당**: BE (문한일) | **인증**: 없음

> 🆕 **v1.1 변경**: 로그인 실패 제한이 IP 기준 단독에서 **IP + code 기준 병합**으로 변경. JWT payload에 `sessionVersion` 추가.

**Request Body**

```json
{ "code": "482917" }
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| code | string | ✅ | 숫자 6자리 정확히 |

**Response 200**

```json
{
  "success": true,
  "data": {
    "childId": "3f1a2b4c-...",
    "nickname": "민준",
    "sessionToken": "eyJhbGci...",
    "profileImageType": "DEFAULT",
    "totalXp": 0
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| code 누락 | 400 | BAD_REQUEST | 코드를 입력해주세요 |
| code가 숫자 6자리 아님 | 400 | BAD_REQUEST | 올바른 형식의 코드를 입력해주세요 |
| 일치하는 코드 없음 | 404 | NOT_FOUND | 코드를 다시 확인해봐요 |
| 30초 잠금 중 요청 | 429 | TOO_MANY_REQUESTS | 잠시 후 다시 시도해주세요 (N초 남음) |

**구현 노트** (v1.1 수정)

```
1. 로그인 실패 제한 — IP + code 이중 체크
   - Redis key: "login_fail:ip:{ip}", "login_fail:code:{code}"
   - 둘 중 하나라도 잠금 상태면 429 반환
   - 3회 연속 실패 시 30초 잠금
2. child_profiles.code 조회 → 없으면 실패 카운트 증가 후 404 반환
3. JWT 세션 토큰 발급
   - payload: { childId, type: "CHILD", sessionVersion }
   - sessionVersion = child_profiles.session_version
   - exp: 30d
4. 성공 시 관련 실패 카운터 초기화
```

---

## PUT /parent/child/{childId}/regenerate-code

> 자녀 코드 재발급 — 기존 세션 강제 만료

**담당**: BE (문한일) | **인증**: PARENT

> 🆕 **v1.1 변경**: Redis 블랙리스트 방식 **제거**, `session_version` 증가 방식으로 교체.

**Path Parameters**

| 파라미터 | 설명 |
| --- | --- |
| childId | 자녀 프로필 UUID |

**Response 200**

```json
{
  "success": true,
  "data": { "newCode": "719284" }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 본인 자녀가 아닌 childId 접근 | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| childId 존재하지 않음 | 404 | NOT_FOUND | 자녀 프로필을 찾을 수 없습니다 |
| 신규 코드 생성 충돌 5회 초과 | 500 | INTERNAL_ERROR | 코드 생성에 실패했습니다 |

**구현 노트** (v1.1 수정)

```
1. Firebase 토큰 검증 → parent_accounts.id 조회
2. child_profiles.parent_id == parent_accounts.id 검증 (불일치 시 403)
3. 새 6자리 코드 생성 후 child_profiles.code 업데이트
4. child_profiles.session_version += 1  ← v1.1 핵심 변경
5. 이후 모든 CHILD 토큰 인증 시:
   - JWT의 sessionVersion == DB의 child_profiles.session_version 검증
   - 불일치 시 401 반환

❌ 삭제된 로직: Redis "revoked_child:{childId}" 블랙리스트 방식 제거
```

---

# 🎯 미션 / 퀴즈 API

## GET /missions

> 전체 미션 목록 + 단계별 잠금 상태 조회

**담당**: BE (문한일) | **인증**: CHILD

**Response 200**

```json
{
  "success": true,
  "data": {
    "missions": [
      {
        "id": "uuid",
        "stage": 1,
        "title": "AI가 뭐예요?",
        "description": "AI의 개념과 할루시네이션을 배워요",
        "isUnlocked": true,
        "isCompleted": true,
        "completedAt": "2026-03-28",
        "isReviewable": true
      },
      {
        "id": "uuid",
        "stage": 2,
        "title": "AI 잘 쓰기",
        "description": "프롬프트 작성법을 배워요",
        "isUnlocked": false,
        "isCompleted": false,
        "completedAt": null,
        "isReviewable": false
      }
    ],
    "stageProgress": {
      "stage1Completed": 3,
      "stage2Completed": 0,
      "stage3Completed": 0
    }
  }
}
```

**잠금 조건 로직**

```
stage 1: 항상 isUnlocked = true
stage 2: stageProgress.stage1Completed >= 3 이면 isUnlocked = true
stage 3: stageProgress.stage2Completed >= 4 이면 isUnlocked = true
```

> **주의**: `stageXCompleted`는 `is_review = false`인 완료만 집계 (v1.1 기준)

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 인증 토큰 없음/만료 | 401 | UNAUTHORIZED | 로그인이 필요합니다 |

---

## GET /missions/daily

> 오늘의 AI 미션 조회 — 당일 GPT 생성 미션 (별도 테이블 daily_missions)

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.2 신규**: 기존 GET /missions와 별도로 오늘의 AI 미션을 조회하는 전용 엔드포인트.
>
> ERD의 `daily_missions` + `daily_mission_questions` 테이블을 사용.

**Response 200 (오늘 미션 있음)**

```json
{
  "success": true,
  "data": {
    "dailyMissionId": "uuid",
    "missionDate": "2026-04-05",
    "generatedBy": "GPT",
    "status": "GENERATED",
    "isCompleted": false,
    "completedAt": null,
    "questions": [
      {
        "id": "q_d01",
        "type": "OX",
        "question": "AI가 생성한 이미지를 실제 사진이라고 믿어도 된다",
        "options": null
      }
    ],
    "questionCount": 5
  }
}
```

**Response 200 (오늘 미션 아직 미생성)**

```json
{
  "success": true,
  "data": {
    "dailyMissionId": null,
    "missionDate": "2026-04-05",
    "status": "NOT_READY",
    "message": "오늘의 미션이 아직 준비 중이에요! 잠시 후 다시 확인해주세요"
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 인증 실패 | 401 | UNAUTHORIZED | 로그인이 필요합니다 |

**구현 노트**

```
1. today(KST) 기준으로 daily_missions 테이블에서 mission_date = today 조회
2. 없으면 NOT_READY 응답 반환
3. 있으면 daily_mission_questions → question_bank 조인하여 5문항 반환
   - answer, explanation 컬럼 SELECT 제외 (정답 비공개)
4. 오늘 해당 자녀가 이미 완료했으면 isCompleted: true
   (mission_daily_progress 또는 mission_attempts 기준)
```

---

## GET /missions/{missionId}/questions

> 미션 문제 조회 — **반드시 정확히 5문항 반환**, answer/explanation 절대 미포함

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.1 변경**: `quizAttemptId` 추가 반환. 이후 제출 시 이 값 필수.
>
> **이유**: 조회한 정확한 5문항만 제출 가능하도록 서버에서 세션을 추적함.

**Path Parameters**

| 파라미터 | 설명 |
| --- | --- |
| missionId | 미션 UUID |

**Response 200**

```json
{
  "success": true,
  "data": {
    "missionId": "uuid",
    "missionTitle": "AI가 뭐예요?",
    "isReview": false,
    "quizAttemptId": "uuid",
    "questionCount": 5,
    "expiresAt": "2026-03-29T10:00:00Z",
    "questions": [
      {
        "id": "q_001",
        "type": "OX",
        "question": "AI는 항상 정확한 정보를 말한다",
        "options": null
      },
      {
        "id": "q_002",
        "type": "MULTIPLE",
        "question": "AI 할루시네이션이란 무엇인가요?",
        "options": ["사실인 정보를 말함", "없는 정보를 사실처럼 말함", "질문을 거부함", "인터넷 검색을 함"]
      },
      {
        "id": "q_003",
        "type": "FILL",
        "question": "AI에게 명확하고 구체적인 질문을 하는 것을 _____ 작성이라고 한다",
        "options": ["프롬프트", "알고리즘", "데이터", "코드"]
      },
      {
        "id": "q_004",
        "type": "SITUATION",
        "question": "친구가 AI에게 숙제를 대신 해달라고 했어요. 어떻게 할까요?",
        "options": ["나도 따라한다", "AI에게 힌트만 받도록 권유한다", "선생님에게 알린다"]
      },
      {
        "id": "q_005",
        "type": "MULTIPLE",
        "question": "AI 답변을 바로 믿기 전에 해야 할 일은 무엇인가요?",
        "options": ["그대로 제출한다", "다른 자료로 확인한다", "친구에게만 물어본다", "아무것도 안 한다"]
      }
    ]
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| missionId 존재하지 않음 | 404 | NOT_FOUND | 미션을 찾을 수 없습니다 |
| 잠긴 미션 접근 시도 | 403 | FORBIDDEN | 아직 잠긴 미션이에요. 이전 단계를 먼저 완료해주세요 |
| 5문항 준비 실패 (GPT 포함) | 500 | INTERNAL_ERROR | 문제 세트를 준비하는 데 실패했습니다 |

**구현 노트** (v1.1 수정)

```
1. 잠금 조건 검증: mission_progress에서 이전 단계 완료 수(is_review=false) COUNT 조회
2. questions 테이블에서 missionId 기준 조회 (answer, explanation 컬럼 SELECT 제외)
3. 문제 수 >= 5개 → 랜덤 5개 선택
4. 문제 수 < 5개 → GPT API로 부족한 수만큼 생성 후 DB 저장
5. 최종 후보가 정확히 5개가 아니면 → 500 반환 (부분 반환 절대 금지)
6. 오늘 이미 is_review=false로 완료한 이력 → isReview: true 반환
7. quiz_attempts 레코드 생성:
   - id, child_id, mission_id
   - question_ids_json (정확히 5개)
   - created_at, expires_at (권장 30분), submitted_at = null
8. 응답에 quizAttemptId, questionCount, expiresAt 포함
```

---

## POST /missions/{missionId}/submit

> 정답 제출 + 서버 채점 + 연쇄 이벤트 전체 처리

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.1 변경**: `quizAttemptId` 필드 필수 추가. `ticketEarned` 필드 제거 → `rewards[]` 배열로 통일.
>
> 🆕 **v1.2 변경**: `Idempotency-Key` 헤더 명시. 복습 시 펫 XP 비적립 확정.

**Request Headers** (v1.2 추가)

```
Idempotency-Key: {uuid}   // 선택. 오프라인 재전송 중복 방지용. 없으면 서버가 자동 생성
```

**Request Body**

```json
{
  "quizAttemptId": "uuid",
  "answers": [
    { "questionId": "q_001", "selected": "false" },
    { "questionId": "q_002", "selected": "없는 정보를 사실처럼 말함" },
    { "questionId": "q_003", "selected": "프롬프트" },
    { "questionId": "q_004", "selected": "AI에게 힌트만 받도록 권유한다" },
    { "questionId": "q_005", "selected": "다른 자료로 확인한다" }
  ]
}
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| quizAttemptId | string | ✅ | 본인에게 발급된 유효한 attempt UUID |
| answers | array | ✅ | 정확히 5개, 중복 questionId 불가 |
| answers[].questionId | string | ✅ | 해당 attempt에 포함된 문제여야 함 |
| answers[].selected | string | ✅ | 선택한 답안 |

**Response 200**

```json
{
  "success": true,
  "data": {
    "score": 4,
    "total": 5,
    "isPerfect": false,
    "xpEarned": 10,
    "equippedPetXp": 95,
    "petStage": "GROWTH",
    "petEvolved": false,
    "crownUnlocked": false,
    "crownType": null,
    "streakDays": 5,
    "todayMissionCount": 1,
    "rewards": [],
    "remainingTickets": {
      "normal": 2,
      "rare": 0,
      "epic": 1
    },
    "profileImageType": "SPROUT",
    "profileImageUnlocked": false,
    "isReview": false,
    "results": [
      {
        "questionId": "q_001",
        "isCorrect": true,
        "explanation": "AI는 할루시네이션이 발생할 수 있어요"
      }
    ]
  }
}
```

> **FE 주의**: v1.0의 `ticketEarned` 필드는 제거됨. 티켓 보상은 `rewards[]` 배열에서 확인.
>
> **FE 주의**: v1.2에서 `newEggUnlocked` 필드가 완전히 제거됨. 아이몽 달성 시 `petEvolved: true` + `rewards[]`로 확인.

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| missionId 존재하지 않음 | 404 | NOT_FOUND | 미션을 찾을 수 없습니다 |
| quizAttemptId 누락 | 400 | BAD_REQUEST | 문제 세션 정보가 필요합니다 |
| answers 배열 비어있음 또는 5개 아님 | 400 | BAD_REQUEST | 답안은 5개를 모두 제출해주세요 |
| answers 내 questionId 중복 | 400 | BAD_REQUEST | 같은 문제를 중복 제출할 수 없어요 |
| 유효하지 않거나 만료된 quizAttemptId | 400 | BAD_REQUEST | 문제 세션이 만료되었어요. 다시 문제를 불러와주세요 |
| 이미 제출 완료된 quizAttemptId | 409 | CONFLICT | 이미 제출한 문제 세트예요 |
| quizAttempt가 해당 child/mission 소속 아님 | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| 잠긴 미션 제출 시도 | 403 | FORBIDDEN | 아직 잠긴 미션이에요 |
| 오늘 이미 완료한 미션 재제출 | 200 | - | isReview: true로 정상 응답 (XP 절반, 펫 XP/보상 없음) |
| DB 트랜잭션 실패 | 500 | INTERNAL_ERROR | 결과 저장에 실패했습니다. 다시 시도해주세요 |

**구현 노트** (v1.2 수정 — 트랜잭션으로 전체 처리)

```
Idempotency-Key 헤더 처리:
  - 헤더 있으면: mission_attempts.idempotency_key로 기존 제출 확인 → 있으면 이전 응답 반환
  - 헤더 없으면: 서버에서 UUID 생성

트랜잭션 시작

① quiz_attempts를 FOR UPDATE로 조회
   - 본인 child인지 확인
   - missionId 일치 확인
   - submitted_at IS NULL 확인
   - expires_at > now() 확인

② answers.questionId 집합이 attempt의 question_ids_json과 정확히 일치하는지 검증

③ questions 테이블에서 attempt의 5문항에 대해서만 answer, explanation 조회

④ 채점
   - score = answers[].selected vs questions.answer 비교
   - isPerfect = (score == 5)

⑤ isReview 판정 (submit 시점 기준)
   - mission_progress에 (childId, missionId, today(KST), is_review=false) 존재 시 → true
   - 없으면 → false

⑥ XP 계산
   - 기본 xpEarned = 10
   - isPerfect → +10
   - isReview → Math.floor(xpEarned * 0.5)
   - 공동 스트릭 보너스: !isReview일 때만, 파트너 오늘 완료 시 * 1.5

⑦ 공통 XP 적립 함수 호출
   - isReview = true면 펫 XP 비적립  ← v1.2 수정

⑧ 펫 성장 단계 체크 (equippedPetId != null && !isReview)

⑨ 펫 만렙(AIMONG) 달성 시 rewards[]에 왕관/티켓 추가
   - NORMAL → rare +1
   - RARE → epic +1
   - EPIC → epic +2
   - LEGEND → epic +3

⑩ 스트릭 업데이트 (!isReview)
   today = LocalDate.now(Asia/Seoul)
   if streak_records.last_completed_date != today:
     streak_records.today_mission_count = 0
   last = streak_records.last_completed_date
   if last == null || last < today.minusDays(1):
     continuous_days = 1
   else if last == today.minusDays(1):
     continuous_days += 1
   // last == today 이면 continuous_days 유지
   streak_records.last_completed_date = today
   streak_records.today_mission_count += 1
   todayFirstMission = (today_mission_count == 1)

⑪ 스트릭 마일스톤 보상 (todayFirstMission && !isReview)
   - 7일: rewards[] += rare +1
   - 30일: rewards[] += epic +1
   - 사용자 목표 달성 체크: streak_milestones에서 achieved=false & target_days == continuous_days
     → achieved = true, achieved_at = now() 업데이트

⑫ mission_attempts INSERT
   { child_id, mission_id, attempt_date: today_KST, attempt_no, score, total: 5,
     xp_earned, is_review, idempotency_key }
   mission_daily_progress UPSERT
   { child_id, mission_id, progress_date, best_score, first_xp_earned, review_attempt_count }

⑬ quiz_attempts.submitted_at = now() 업데이트

트랜잭션 커밋
```

---

# 🐣 펫 API

## GET /pet

> 현재 장착 펫 + 보유 펫 전체 목록 조회

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.2 변경**: 응답에 `mood` 필드 추가. FE가 펫 감정 상태를 서버에서 받도록 개선.

**Response 200**

```json
{
  "success": true,
  "data": {
    "equippedPet": {
      "id": "uuid",
      "petType": "pet_normal_001",
      "grade": "NORMAL",
      "xp": 95,
      "stage": "GROWTH",
      "mood": "HAPPY",
      "crownUnlocked": false,
      "crownType": null,
      "obtainedAt": "2026-03-25T10:00:00Z"
    },
    "pets": [
      {
        "id": "uuid",
        "petType": "pet_rare_003",
        "grade": "RARE",
        "xp": 0,
        "stage": "EGG",
        "mood": "IDLE",
        "crownUnlocked": true,
        "crownType": "gold",
        "obtainedAt": "2026-03-27T15:30:00Z"
      }
    ],
    "totalPetCount": 3
  }
}
```

**mood 값 정의** (v1.2 추가)

| mood | 조건 | 표현 |
| --- | --- | --- |
| HAPPY | 오늘 미션 완료 (daysSinceLastMission == 0 & 완료) | 기쁜 표정 |
| IDLE | 오늘 미션 미완료 (daysSinceLastMission == 0 & 미완료) | 기본 표정 |
| SAD_LIGHT | daysSinceLastMission == 1 | 눈물방울 |
| SAD_DEEP | daysSinceLastMission >= 2 | 회색 + 슬픈 표정 |

> `equippedPet`이 null이면 FE에서 "펫을 장착해야 XP를 얻을 수 있어요!" 안내 표시
>
> MVP에서는 스타터 가챠 후 첫 번째 펫 자동 장착

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 인증 실패 | 401 | UNAUTHORIZED | 로그인이 필요합니다 |

**구현 노트** (v1.2 추가)

```
mood 계산 (DB의 pets.mood 값을 그대로 반환):
  - pets.mood는 매일 00:01 KST 스케줄러가 업데이트
  - 미션 완료 시 즉시 HAPPY로 업데이트
  - FE에서 별도로 계산하지 않아도 됨
```

---

## PUT /pet/equip

> 장착 펫 변경 — 보유 중인 펫을 장착 펫으로 교체

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.2 신규**: MVP에서 스타터 가챠 후 자동 장착 이후, 2번째 펫부터 장착 변경이 필요하여 추가.

**Request Body**

```json
{ "petId": "uuid" }
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| petId | string | ✅ | 본인이 보유한 펫의 UUID |

**Response 200**

```json
{
  "success": true,
  "data": {
    "equippedPet": {
      "id": "uuid",
      "petType": "pet_rare_003",
      "grade": "RARE",
      "xp": 0,
      "stage": "EGG",
      "mood": "IDLE",
      "crownUnlocked": false,
      "crownType": null,
      "obtainedAt": "2026-03-27T15:30:00Z"
    }
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| petId 누락 | 400 | BAD_REQUEST | 장착할 펫을 선택해주세요 |
| 본인 소유 펫이 아님 | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| petId 존재하지 않음 | 404 | NOT_FOUND | 펫을 찾을 수 없습니다 |
| 이미 장착 중인 펫 | 409 | CONFLICT | 이미 장착 중인 펫이에요 |

**구현 노트**

```
1. pets에서 (id = petId, child_id = 나) 확인 → 없으면 403/404
2. equipped_pets에서 child_id = 나 인 기존 레코드 확인
3. 이미 같은 petId이면 409
4. 트랜잭션:
   - equipped_pets UPSERT (child_id PK → INSERT ON CONFLICT UPDATE)
     { child_id, pet_id: petId, equipped_at: now() }
5. 커밋

⚠️ 장착 변경 시 기존 펫/신규 펫 XP 초기화 없음 — 각 펫의 xp 유지
```

---

# 🤖 챗봇 API

## POST /chat/send

> GPT 챗봇 메시지 전송 (초등학생 전용 안전 필터 적용)

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.1 변경**: `masked` 필드 의미 변경 — FE 마스킹 여부 참고용이며, **서버는 이 값을 신뢰하지 않고 자체 2차 마스킹 수행**. 원문 메시지는 DB/로그 저장 금지.

**Request Body**

```json
{
  "message": "광합성이 뭐야?",
  "masked": false
}
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| message | string | ✅ | 1~200자, FE는 마스킹 처리본 전송 권장 |
| masked | boolean | ✅ | FE 마스킹 여부. 서버는 이 값 신뢰 안 하고 추가 처리 |

**Response 200**

```json
{
  "success": true,
  "data": {
    "reply": "광합성은 식물이 햇빛을 받아서 이산화탄소와 물로 포도당을 만드는 과정이에요!",
    "remainingCalls": 17,
    "hintSuggestion": null
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| message 누락 또는 빈 문자열 | 400 | BAD_REQUEST | 메시지를 입력해주세요 |
| message 200자 초과 | 400 | BAD_REQUEST | 메시지는 200자 이하로 입력해주세요 |
| masked 필드 누락 | 400 | BAD_REQUEST | masked 필드가 필요합니다 |
| 일일 20회 한도 초과 | 429 | TOO_MANY_REQUESTS | 오늘은 충분히 이야기했어요! 내일 또 만나요 |
| GPT 응답 15초 초과 | 504 | GATEWAY_TIMEOUT | AI 친구가 생각 중이에요. 다시 시도해볼까요? |
| GPT API 오류 (5xx) | 500 | INTERNAL_ERROR | AI 친구가 지금 쉬고 있어요. 잠시 후 다시 시도해주세요 |

**구현 노트** (v1.1 수정)

```
1. chat_usage에서 오늘(KST) 성공 호출 수 조회 → >= 20이면 429
2. 서버 자체 2차 정규식 기반 개인정보 마스킹 수행
3. 외부 GPT에는 항상 sanitizedMessage만 전달
4. 원문 메시지는 DB/로그에 절대 저장 금지
5. 서버 2차 검사에서 개인정보 감지 시 → 감지 유형만 privacy_events에 기록 (원문 저장 금지)
6. OpenAI 호출 성공 시에만 chat_usage.count += 1
   (timeout / 5xx / validation 실패는 차감하지 않음)
7. 힌트 트리거: message에 "숙제", "해줘", "대신", "써줘" 포함 시 hintSuggestion 세팅
8. 데일리 퀘스트 CHAT_GPT → 오늘 첫 성공 호출 시 자동 완료 + XP 5 자동 지급 (AUTO)
9. 위클리 퀘스트 CHAT_3 진행도 → 성공 호출 기준으로만 증가
```

---

# 🛡️ 개인정보 API

## POST /privacy/event

> 개인정보 감지 이벤트 기록 + 부모 FCM 비동기 발송

**담당**: BE (문한일) | **인증**: CHILD

> ⚠️ FE에서 온디바이스(ML Kit + Regex)로 감지 후 호출. 원문 텍스트는 절대 전송/저장 안 함.

**Request Body**

```json
{
  "detectedType": "NAME",
  "masked": true
}
```

| detectedType | 설명 |
| --- | --- |
| NAME | 이름 감지 |
| SCHOOL | 학교명 감지 |
| AGE | 나이/학년 감지 |
| PHONE | 전화번호 감지 |
| EMAIL | 이메일 감지 |
| ADDRESS | 주소 감지 |
| DATE | 날짜(생년월일 추정) |
| URL | URL 감지 |
| ETC | 기타 개인정보 의심 패턴 감지 ← v1.2 추가 (ERD ENUM과 일치) |

**Response 200**

```json
{
  "success": true,
  "data": { "recorded": true }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| detectedType 누락 | 400 | BAD_REQUEST | 감지 유형을 입력해주세요 |
| 유효하지 않은 detectedType 값 | 400 | BAD_REQUEST | 올바른 감지 유형이 아닙니다 |
| masked 필드 누락 | 400 | BAD_REQUEST | masked 필드가 필요합니다 |

**구현 노트**

```
1. privacy_events INSERT
   { child_id, detected_type, masked }  ← 원문(텍스트) 절대 저장 안 함

2. FCM 발송 (비동기 @Async)
   2단계 제한 체크:
     1단계: 오늘 해당 자녀의 PRIVACY_ALERT 발송 횟수 < 5회
     2단계: 오늘 해당 부모 계정의 전체 FCM 발송 횟수 < 5회
   초과 시: 큐 적재 후 다음 발송 시 묶음 발송

   FCM Payload:
   {
     "title": "개인정보 입력 감지",
     "body": "자녀가 AI에게 개인정보를 입력하려 했어요. 대화해보세요 😊",
     "data": { "type": "PRIVACY_ALERT", "childId": "...", "detectedType": "NAME" }
   }
   ※ 원문 내용 부모에게도 절대 공유 안 함
```

---

# 🎰 가챠 API

## POST /gacha/pull

> 가챠 뽑기 실행 — 난수 생성 반드시 서버에서

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.1 변경**: `srBonus`는 이제 **실제 적용치(appliedSrBonus)** 기준으로 반환. 오버플로우 방지.

**Request Body**

```json
{ "ticketType": "NORMAL" }
```

| ticketType | 보장 등급 | 설명 |
| --- | --- | --- |
| NORMAL | 없음 | 기본 확률표 그대로 |
| RARE | 희귀 이상 | 일반 0% 고정 후 나머지 재정규화 |
| EPIC | 영웅 이상 | 일반+희귀 0% 고정 후 나머지 재정규화 |

**Response 200**

```json
{
  "success": true,
  "data": {
    "result": {
      "petId": "uuid",
      "petType": "pet_rare_003",
      "petName": "번개몽",
      "grade": "RARE",
      "isNew": false,
      "fragmentsGot": 3
    },
    "srMissCount": 8,
    "srBonus": 0,
    "levelUp": false,
    "remainingTickets": {
      "normal": 2,
      "rare": 0,
      "epic": 1
    }
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| ticketType 누락 | 400 | BAD_REQUEST | 티켓 종류를 선택해주세요 |
| 유효하지 않은 ticketType 값 | 400 | BAD_REQUEST | 올바른 티켓 종류가 아닙니다 |
| 해당 티켓 보유량 0장 | 400 | BAD_REQUEST | 티켓이 부족해요! |
| DB 트랜잭션 실패 | 500 | INTERNAL_ERROR | 뽑기 처리에 실패했습니다. 티켓은 차감되지 않았습니다 |

**구현 노트** (v1.1 수정 — 트랜잭션으로 묶기)

```
트랜잭션 시작

1. tickets row를 FOR UPDATE로 조회
2. 해당 티켓 보유량 확인 → 부족하면 400
3. 티켓 차감
4. child_profiles row를 FOR UPDATE로 조회
5. gacha_pull_count += 1
6. 확률 구간 결정 (gacha_pull_count 기준):
   0~19:  Lv.1 (일반75% 희귀21% 영웅3.5% 전설0.5%)
   20~49: Lv.2 (일반66% 희귀24% 영웅7.5% 전설2.5%)
   50~99: Lv.3 (일반56% 희귀27% 영웅13% 전설4%)
   100+:  Lv.4 (일반44% 희귀29% 영웅22% 전설5%)

7. srBonus 계산 (v1.1 수정)
   requestedBonus = Math.floor(sr_miss_count / 10) * 10
   appliedSrBonus = min(requestedBonus, baseNormalProbability)
   heroPlus = baseHeroPlus + appliedSrBonus
   normal = baseNormalProbability - appliedSrBonus
   ← 응답의 srBonus는 appliedSrBonus 값을 반환

8. ticketType에 따라 재정규화
9. SecureRandom으로 가중 랜덤 등급 결정
10. sr_miss_count 업데이트
    - 영웅 이상: sr_miss_count = 0
    - 그 외: sr_miss_count += 1
11. 레벨업 체크 → tickets.normal += 2, shield_count += 1, FCM 비동기 발송
12. 중복 여부 확인 (UNIQUE: child_id, pet_type 기준)
    - 신규: pets INSERT (xp=0, stage=EGG, mood=IDLE)
    - 중복: fragments.count += 등급별 조각 수 (NORMAL+1, RARE+3, EPIC+8, LEGEND+20)
13. gacha_pulls INSERT

트랜잭션 커밋 → FCM 비동기 발송
```

---

## GET /gacha/fragments

> 조각 보유 현황 조회

**담당**: BE (문한일) | **인증**: CHILD

**Response 200**

```json
{
  "success": true,
  "data": {
    "fragments": [
      { "grade": "NORMAL",  "count": 7,  "exchangeThreshold": 10  },
      { "grade": "RARE",    "count": 12, "exchangeThreshold": 30  },
      { "grade": "EPIC",    "count": 0,  "exchangeThreshold": 80  },
      { "grade": "LEGEND",  "count": 0,  "exchangeThreshold": 200 }
    ]
  }
}
```

---

## POST /gacha/exchange

> 조각으로 원하는 신규 펫 교환

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.1 변경**: **이미 보유 중인 펫은 교환 불가**. 신규 펫 획득 전용 API로 정의.

**Request Body**

```json
{
  "grade": "NORMAL",
  "petType": "pet_normal_005"
}
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| grade | string | ✅ | NORMAL / RARE / EPIC / LEGEND |
| petType | string | ✅ | 유효한 펫 종류 코드, grade와 등급 일치 |

**Response 200**

```json
{
  "success": true,
  "data": {
    "pet": {
      "id": "uuid",
      "petType": "pet_normal_005",
      "grade": "NORMAL",
      "xp": 0,
      "stage": "EGG"
    },
    "remainingFragments": 0
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| grade 또는 petType 누락 | 400 | BAD_REQUEST | 등급과 펫 종류를 선택해주세요 |
| 유효하지 않은 petType 코드 | 400 | BAD_REQUEST | 올바른 펫 종류가 아닙니다 |
| grade와 petType의 등급 불일치 | 400 | BAD_REQUEST | 펫 등급이 일치하지 않습니다 |
| 조각 부족 | 400 | BAD_REQUEST | 조각이 부족해요! (현재 N개, 필요 M개) |
| 이미 보유 중인 petType 교환 시도 | 409 | CONFLICT | 이미 보유 중인 펫이에요 |

**구현 노트** (v1.1 수정)

```
1. fragments에서 (child_id, grade) row를 FOR UPDATE로 조회
2. count < threshold → 400
3. pets에서 (child_id, pet_type) 존재 여부 확인 → 이미 보유면 409
4. 트랜잭션: fragments.count -= threshold, pets INSERT (xp=0, stage=EGG, mood=IDLE)
```

---

# 🔥 스트릭 API

## GET /streak

> 스트릭 현황 + 공동 스트릭 파트너 정보 조회

**담당**: BE (문한일) | **인증**: CHILD

**Response 200**

```json
{
  "success": true,
  "data": {
    "continuousDays": 5,
    "lastCompletedDate": "2026-03-28",
    "todayMissionCount": 1,
    "shieldCount": 2,
    "partner": {
      "childId": "uuid",
      "nickname": "지우",
      "todayCompleted": true
    }
  }
}
```

**응답 규칙** (v1.1 추가)

```
- lastCompletedDate != today(KST) 이면 → todayMissionCount: 0 반환 (저장값 무시)
- partner.todayCompleted = true 조건:
    파트너의 lastCompletedDate == today(KST)
    AND 파트너의 todayMissionCount > 0
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 인증 실패 | 401 | UNAUTHORIZED | 로그인이 필요합니다 |

---

## POST /streak/partner

> 공동 스트릭 파트너 연결

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.1 변경**: Race condition 방지를 위해 두 child row를 **항상 같은 순서로** FOR UPDATE 후 재확인.

**Request Body**

```json
{ "partnerCode": "719284" }
```

**Response 200**

```json
{
  "success": true,
  "data": {
    "partner": {
      "childId": "uuid",
      "nickname": "지우"
    }
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| partnerCode 누락 | 400 | BAD_REQUEST | 친구 코드를 입력해주세요 |
| 본인 코드 입력 | 400 | BAD_REQUEST | 본인의 코드는 입력할 수 없어요 |
| 코드에 해당하는 자녀 없음 | 404 | NOT_FOUND | 코드를 다시 확인해봐요 |
| 이미 파트너 연결됨 (내가 이미 연결) | 409 | CONFLICT | 이미 친구와 연결되어 있어요 |
| 상대방도 이미 다른 파트너 연결됨 | 409 | CONFLICT | 친구가 이미 다른 친구와 연결되어 있어요 |

**구현 노트** (v1.1 수정)

```
1. partnerCode로 파트너 child 조회 → 없으면 404
2. 본인 코드 여부 확인 → 400
3. 트랜잭션 시작
4. 두 child_id를 항상 같은 순서(UUID 오름차순)로 FOR UPDATE  ← v1.1 핵심 (deadlock 방지)
5. 내가 이미 파트너 연결되어 있는지 재확인 → 있으면 409
6. 상대도 이미 파트너 연결되어 있는지 재확인 → 있으면 409
7. friend_streaks INSERT 2행 (A→B, B→A 동시)
8. 커밋
```

---

## DELETE /streak/partner

> 공동 스트릭 파트너 연결 해제

**인증**: CHILD

**Response 200**

```json
{
  "success": true,
  "data": { "disconnected": true }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 연결된 파트너 없음 | 404 | NOT_FOUND | 연결된 친구가 없어요 |

**구현 노트** (v1.2 수정 — ERD 대칭 2행 모델 기준으로 정정)

```
1. friend_streaks에서 child_id = 나 인 레코드 조회
2. 없으면 404
3. 트랜잭션:
   - friend_streaks DELETE WHERE child_id = 나        ← 내 행 삭제
   - friend_streaks DELETE WHERE child_id = 파트너ID  ← 파트너 행 삭제 (대칭 삭제)
   (ON DELETE CASCADE가 설정된 경우 한쪽만 삭제해도 자동 처리될 수 있으나,
    명시적으로 두 행을 삭제하는 것이 더 안전함)
4. 커밋
5. 상대방에게 FCM: "파트너 연결이 끊겼어요" (비동기)

⚠️ v1.1의 잘못된 구현 노트 수정:
   기존: (child_a_id = 나 OR child_b_id = 나)  ← 존재하지 않는 컬럼명
   수정: child_id = 나  ← ERD의 실제 컬럼명 (대칭 2행 모델)
```

---

## GET /streak/milestones

> 사용자 목표 마일스톤 조회 (30일 이후 설정 가능)

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.2 신규**: ERD의 `streak_milestones` 테이블 및 기능명세서 섹션 10-2 반영.

**Response 200**

```json
{
  "success": true,
  "data": {
    "continuousDays": 35,
    "milestones": [
      {
        "id": "uuid",
        "targetDays": 55,
        "tier": 1,
        "achieved": false,
        "rewardClaimed": false,
        "achievedAt": null,
        "reward": { "type": "TICKET", "ticketType": "NORMAL", "count": 1 }
      },
      {
        "id": "uuid",
        "targetDays": 80,
        "tier": 2,
        "achieved": false,
        "rewardClaimed": false,
        "achievedAt": null,
        "reward": { "type": "TICKET", "ticketType": "RARE", "count": 1 }
      }
    ]
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 인증 실패 | 401 | UNAUTHORIZED | 로그인이 필요합니다 |

---

## POST /streak/milestone

> 사용자 목표 마일스톤 설정

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.2 신규**: 30일 초과, 25일 단위로 사용자가 직접 목표 설정. 기능명세서 섹션 10-2 반영.

**Request Body**

```json
{
  "targetDays": 55,
  "tier": 1
}
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| targetDays | int | ✅ | 31 이상, (targetDays - 30) % 25 == 0 이어야 함 (예: 55, 80, 105...) |
| tier | int | ✅ | 1, 2, 3 중 하나 |

**tier별 보상**

| tier | 보상 |
| --- | --- |
| 1 | 일반 티켓 1장 |
| 2 | 레어 티켓 1장 |
| 3 | 레어 티켓 3장 |

**Response 201**

```json
{
  "success": true,
  "data": {
    "milestoneId": "uuid",
    "targetDays": 55,
    "tier": 1,
    "reward": { "type": "TICKET", "ticketType": "NORMAL", "count": 1 }
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| targetDays 누락 또는 30 이하 | 400 | BAD_REQUEST | 목표 일수는 30일 초과여야 해요 |
| targetDays가 25일 단위 아님 | 400 | BAD_REQUEST | 목표 일수는 55, 80, 105일 등 25일 단위로 설정해주세요 |
| tier 누락 또는 1/2/3 아님 | 400 | BAD_REQUEST | 올바른 단계를 선택해주세요 |
| 동일 targetDays 이미 설정됨 | 409 | CONFLICT | 이미 설정된 목표예요 |

**구현 노트**

```
1. targetDays 유효성: > 30 && (targetDays - 30) % 25 == 0
2. streak_milestones에서 (child_id, target_days) 중복 확인 → 있으면 409
3. FOR UPDATE로 동시 중복 방지
4. streak_milestones INSERT
   { child_id, target_days, tier, achieved: false, reward_claimed: false }
```

---

## POST /streak/milestone/claim

> 달성한 목표 마일스톤 보상 수령

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.2 신규**

**Request Body**

```json
{ "milestoneId": "uuid" }
```

**Response 200**

```json
{
  "success": true,
  "data": {
    "rewards": [
      { "type": "TICKET", "ticketType": "NORMAL", "count": 1, "reason": "STREAK_MILESTONE" }
    ],
    "remainingTickets": { "normal": 4, "rare": 1, "epic": 0 }
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| milestoneId 누락 | 400 | BAD_REQUEST | 마일스톤 정보가 필요합니다 |
| 본인 마일스톤 아님 | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| milestoneId 존재하지 않음 | 404 | NOT_FOUND | 마일스톤을 찾을 수 없습니다 |
| 아직 달성하지 않은 마일스톤 | 400 | BAD_REQUEST | 아직 달성하지 못한 목표예요 |
| 이미 보상 수령함 | 409 | CONFLICT | 이미 보상을 받았어요 |

**구현 노트**

```
1. streak_milestones에서 id = milestoneId AND child_id = 나 확인 → 없으면 403/404
2. achieved = false → 400
3. reward_claimed = false 여부 FOR UPDATE로 확인
4. tier에 따라 티켓 지급
5. reward_claimed = true, tickets 업데이트
6. 커밋
```

---

# 📊 퀘스트 / 업적 API

## GET /quests/daily

> 오늘의 데일리 퀘스트 목록 + 진행 현황

**담당**: BE (문한일) | **인증**: CHILD

**Response 200**

```json
{
  "success": true,
  "data": {
    "date": "2026-03-29",
    "todayXp": 10,
    "quests": [
      {
        "questType": "MISSION_1",
        "label": "미션 1개 완료하기",
        "reward": "자동 적용(별도 수령 없음)",
        "claimType": "AUTO",
        "completed": true,
        "rewardClaimed": true,
        "progress": { "current": 1, "required": 1 }
      },
      {
        "questType": "CHAT_GPT",
        "label": "GPT 챗봇과 대화하기",
        "reward": "XP 5 자동 지급",
        "claimType": "AUTO",
        "completed": false,
        "rewardClaimed": false,
        "progress": { "current": 0, "required": 1 }
      },
      {
        "questType": "XP_20",
        "label": "오늘 XP 20 획득하기",
        "reward": "일반 티켓 1장",
        "claimType": "MANUAL",
        "completed": false,
        "rewardClaimed": false,
        "progress": { "current": 10, "required": 20 }
      },
      {
        "questType": "ALL_3",
        "label": "데일리 3개 모두 완료",
        "reward": "일반 티켓 1장",
        "claimType": "MANUAL",
        "completed": false,
        "rewardClaimed": false,
        "progress": { "current": 1, "required": 3 }
      }
    ]
  }
}
```

**퀘스트 타입별 규칙** (v1.1)

| questType | claimType | 보상 처리 |
| --- | --- | --- |
| MISSION_1 | AUTO | 미션 완료 자체가 보상. 별도 claim 없음 |
| CHAT_GPT | AUTO | 첫 성공 호출 시 XP 5 자동 지급 |
| XP_20 | MANUAL | 완료 후 `/quests/claim` 호출 필요. 보상: 일반 티켓 1장 |
| ALL_3 | MANUAL | 완료 후 `/quests/claim` 호출 필요. 보상: 일반 티켓 1장 |

> `rewardClaimed`는 AUTO 퀘스트의 경우 completed 시점에 자동으로 true가 됨

---

## GET /quests/weekly

> 이번 주 위클리 퀘스트 목록 + 진행 현황

**담당**: BE (문한일) | **인증**: CHILD

**Response 200**

```json
{
  "success": true,
  "data": {
    "weekStart": "2026-03-23",
    "weeklyXp": 30,
    "quests": [
      {
        "questType": "XP_100",
        "label": "이번 주 XP 100 획득하기",
        "reward": "레어 티켓 1장",
        "claimType": "MANUAL",
        "completed": false,
        "rewardClaimed": false,
        "progress": { "current": 30, "required": 100 }
      },
      {
        "questType": "MISSION_5",
        "label": "미션 5개 완료하기",
        "reward": "일반 티켓 2장",
        "claimType": "MANUAL",
        "completed": false,
        "rewardClaimed": false,
        "progress": { "current": 3, "required": 5 }
      },
      {
        "questType": "CHAT_3",
        "label": "GPT 챗봇 3번 사용하기",
        "reward": "일반 티켓 1장",
        "claimType": "MANUAL",
        "completed": false,
        "rewardClaimed": false,
        "progress": { "current": 1, "required": 3 }
      }
    ]
  }
}
```

> **집계 기준**: `MISSION_5` 진행도는 `is_review = false` 완료만 카운트

---

## POST /quests/claim

> 완료된 퀘스트 보상 수령 (MANUAL 퀘스트만 해당)

**담당**: BE (문한일) | **인증**: CHILD

**Request Body**

```json
{
  "questType": "XP_20",
  "period": "daily"
}
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| questType | string | ✅ | daily manual: XP_20, ALL_3 / weekly manual: XP_100, MISSION_5, CHAT_3 |
| period | string | ✅ | daily 또는 weekly |

**Response 200**

```json
{
  "success": true,
  "data": {
    "rewards": [
      {
        "type": "TICKET",
        "ticketType": "NORMAL",
        "count": 1,
        "reason": "DAILY_QUEST_XP_20"
      }
    ],
    "remainingTickets": {
      "normal": 3,
      "rare": 0,
      "epic": 0
    }
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| questType 또는 period 누락 | 400 | BAD_REQUEST | 퀘스트 정보를 입력해주세요 |
| 유효하지 않은 questType 또는 period | 400 | BAD_REQUEST | 올바른 퀘스트 정보가 아닙니다 |
| period와 questType 조합 불일치 | 400 | BAD_REQUEST | 올바른 퀘스트 정보가 아닙니다 |
| AUTO 퀘스트 수령 시도 | 400 | BAD_REQUEST | 자동 지급 퀘스트는 수령 API를 호출할 수 없어요 |
| 퀘스트 미완료 상태에서 수령 시도 | 400 | BAD_REQUEST | 아직 완료되지 않은 퀘스트예요 |
| 이미 보상 수령함 | 409 | CONFLICT | 이미 보상을 받았어요 |

**구현 노트** (v1.1 수정)

```
1. period/questType 조합 검증
2. 해당 퀘스트가 MANUAL 타입인지 검증
3. 완료 여부 검증
4. reward_claimed = false 여부를 FOR UPDATE로 확인
5. 지급할 보상을 rewards[]로 구성
6. 티켓 보상 지급 후 reward_claimed = true
7. 커밋
```

---

## GET /achievements

> 히든 프로필 업적 달성 현황

**담당**: BE (문한일) | **인증**: CHILD

**Response 200**

```json
{
  "success": true,
  "data": {
    "profileImageType": "SPROUT",
    "totalXp": 150,
    "achievements": [
      {
        "type": "SPROUT",
        "label": "AI 새싹",
        "requiredXp": 100,
        "unlockedAt": "2026-03-25T10:00:00Z"
      }
    ],
    "locked": [
      { "type": "EXPLORER", "label": "AI 탐험가", "requiredXp": 300 },
      { "type": "CRITIC",   "label": "AI 비평가",  "requiredXp": 500 },
      { "type": "GUARDIAN", "label": "AI 수호자",  "requiredXp": 1000 }
    ],
    "nextThreshold": {
      "type": "EXPLORER",
      "requiredXp": 300,
      "currentXp": 150,
      "remaining": 150
    }
  }
}
```

---

# 💌 복귀 보상 API

## GET /return-reward

> 복귀 보상 확인 — 앱 오픈 시 호출, 자동 지급 아님

**담당**: BE (문한일) | **인증**: CHILD

> 🆕 **v1.1 변경**: 중복 지급 방지 기준이 **"오늘 수령 여부"에서 "같은 결석 구간 수령 여부"** 로 변경.

**Response 200 (보상 있음)**

```json
{
  "success": true,
  "data": {
    "hasReward": true,
    "daysMissed": 4,
    "ticketCount": 2,
    "message": "4일 만에 돌아왔어요! 🎉 티켓 2장 드릴게요!"
  }
}
```

**Response 200 (보상 없음)**

```json
{
  "success": true,
  "data": { "hasReward": false }
}
```

**구현 노트** (v1.1 수정)

```
baseDate = streak_records.last_completed_date
if baseDate == null:
  hasReward = false
else:
  daysMissed = today(KST) - baseDate
  ticketCount = min(daysMissed - 2, 3)
  if daysMissed <= 2:
    hasReward = false
  else if exists return_reward_claims(child_id, base_last_completed_date = baseDate):
    hasReward = false  ← v1.1 변경: 날짜 기준 → 결석 구간 기준
  else:
    hasReward = true
```

---

## POST /return-reward/claim

> 복귀 보상 수령

**담당**: BE (문한일) | **인증**: CHILD

**Response 200**

```json
{
  "success": true,
  "data": {
    "ticketEarned": { "type": "NORMAL", "count": 2 },
    "remainingTickets": { "normal": 5, "rare": 0, "epic": 0 }
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 복귀 보상 조건 미충족 (3일 미만 또는 미션 이력 없음) | 400 | BAD_REQUEST | 복귀 보상이 없어요 |
| 같은 결석 구간에 대한 보상 이미 수령 | 409 | CONFLICT | 이미 보상을 받았어요 |

**구현 노트** (v1.1 수정)

```
1. GET /return-reward와 동일한 조건으로 재검증
2. 트랜잭션 시작
3. tickets row FOR UPDATE
4. return_reward_claims에 (child_id, base_last_completed_date) UNIQUE INSERT
5. tickets.normal += ticketCount
6. 커밋
```

---

# 📊 부모 대시보드 API

## GET /parent/children

> 부모가 자신의 자녀 목록 조회

**담당**: BE (문한일) | **인증**: PARENT

> 🆕 **v1.2 신규**: 부모 대시보드 진입 시 어떤 자녀를 볼지 선택하기 위해 필요.

**Response 200**

```json
{
  "success": true,
  "data": {
    "children": [
      {
        "childId": "uuid",
        "nickname": "민준",
        "code": "482917",
        "profileImageType": "SPROUT",
        "totalXp": 150,
        "lastActiveAt": "2026-04-05T09:30:00Z"
      },
      {
        "childId": "uuid",
        "nickname": "지아",
        "code": "371028",
        "profileImageType": "DEFAULT",
        "totalXp": 0,
        "lastActiveAt": null
      }
    ]
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 인증 실패 | 401 | UNAUTHORIZED | 로그인이 필요합니다 |

**구현 노트**

```
1. Firebase 토큰 검증 → parent_accounts.id 조회
2. child_profiles WHERE parent_id = parent_accounts.id 조회
3. 없으면 children: [] 빈 배열 반환 (404 아님)
```

---

## DELETE /parent/child/{childId}

> 자녀 프로필 삭제 — 연관 데이터 전체 CASCADE DELETE

**담당**: BE (문한일) | **인증**: PARENT

> 🆕 **v1.2 신규**: 기능명세서 EC-04 반영. 자녀 삭제 시 pets, streak, 가챠, 퀘스트 등 모든 데이터 연쇄 삭제.

**Path Parameters**

| 파라미터 | 설명 |
| --- | --- |
| childId | 자녀 프로필 UUID |

**Response 200**

```json
{
  "success": true,
  "data": { "deleted": true }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 본인 자녀가 아닌 childId 접근 | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| childId 존재하지 않음 | 404 | NOT_FOUND | 자녀 프로필을 찾을 수 없습니다 |

**구현 노트**

```
1. Firebase 토큰 검증 → parent_accounts.id 조회
2. child_profiles.parent_id == parent_accounts.id 검증 (불일치 시 403)
3. child_profiles DELETE WHERE id = childId
   → ERD의 CASCADE DELETE 설정으로 연관 테이블 자동 삭제:
     pets, equipped_pets, streak_records, friend_streaks, mission_attempts,
     mission_daily_progress, tickets, fragments, gacha_pulls, daily_quests,
     weekly_quests, achievements, privacy_events, chat_usage,
     quiz_attempts, return_reward_claims, streak_milestones, milestone_rewards

⚠️ 삭제 전 부모에게 "정말 삭제하시겠어요?" 확인 팝업을 FE에서 표시 권장
⚠️ 삭제된 데이터는 복구 불가
```

---

## POST /parent/fcm-token

> 부모 FCM 토큰 등록/갱신

**담당**: BE (문한일) | **인증**: PARENT

> 🆕 **v1.2 신규**: 부모 앱 FCM 알림 발송을 위해 디바이스 토큰을 등록해야 함.
>
> 앱 실행 시 또는 토큰 갱신 시 호출.

**Request Body**

```json
{
  "fcmToken": "dXPXi-...",
  "platform": "ANDROID"
}
```

| 필드 | 타입 | 필수 | 유효성 |
| --- | --- | --- | --- |
| fcmToken | string | ✅ | Firebase FCM 디바이스 토큰 |
| platform | string | ✅ | ANDROID 또는 IOS |

**Response 200**

```json
{
  "success": true,
  "data": { "registered": true }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| fcmToken 또는 platform 누락 | 400 | BAD_REQUEST | 토큰 정보를 입력해주세요 |
| 유효하지 않은 platform | 400 | BAD_REQUEST | 올바른 플랫폼이 아닙니다 |
| 인증 실패 | 401 | UNAUTHORIZED | 로그인이 필요합니다 |

**구현 노트**

```
1. Firebase 토큰 검증 → parent_accounts.id 조회
2. parent_accounts에 fcm_token, platform 컬럼 UPSERT
   - 컬럼이 없으면 parent_accounts 테이블에 추가 필요

ERD 반영 필요: parent_accounts 테이블에 fcm_token TEXT, platform TEXT 컬럼 추가
```

---

## GET /parent/child/{childId}/summary

> 자녀 학습 요약 (스트릭 + XP + 미션 완료 수)

**담당**: BE (문한일) | **인증**: PARENT

**Response 200**

```json
{
  "success": true,
  "data": {
    "nickname": "민준",
    "profileImageType": "SPROUT",
    "totalXp": 150,
    "continuousDays": 5,
    "shieldCount": 2,
    "weeklyMissionCount": 7,
    "totalMissionCount": 12,
    "lastActiveAt": "2026-03-29T09:30:00Z"
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 본인 자녀가 아닌 childId 접근 | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| childId 존재하지 않음 | 404 | NOT_FOUND | 자녀 프로필을 찾을 수 없습니다 |

**구현 노트** (v1.1 추가)

```
집계 기준 (v1.1 명시):
- weeklyMissionCount: mission_progress.is_review = false만 집계
- totalMissionCount: mission_progress.is_review = false만 집계
```

---

## GET /parent/child/{childId}/weekly-stats

> 주간 학습 통계 (막대 그래프용 — 월~일 7일 데이터)

**담당**: BE (문한일) | **인증**: PARENT

**Response 200**

```json
{
  "success": true,
  "data": {
    "weekStart": "2026-03-23",
    "weekEnd": "2026-03-29",
    "totalWeeklyXp": 85,
    "totalWeeklyMissions": 7,
    "dailyStats": [
      { "date": "2026-03-23", "dayOfWeek": "MON", "missionCount": 2, "xpEarned": 20 },
      { "date": "2026-03-24", "dayOfWeek": "TUE", "missionCount": 0, "xpEarned": 0  },
      { "date": "2026-03-25", "dayOfWeek": "WED", "missionCount": 3, "xpEarned": 35 },
      { "date": "2026-03-26", "dayOfWeek": "THU", "missionCount": 1, "xpEarned": 10 },
      { "date": "2026-03-27", "dayOfWeek": "FRI", "missionCount": 1, "xpEarned": 10 },
      { "date": "2026-03-28", "dayOfWeek": "SAT", "missionCount": 0, "xpEarned": 0  },
      { "date": "2026-03-29", "dayOfWeek": "SUN", "missionCount": 0, "xpEarned": 0  }
    ]
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 본인 자녀가 아닌 childId | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| childId 없음 | 404 | NOT_FOUND | 자녀 프로필을 찾을 수 없습니다 |

**구현 노트** (v1.1 추가)

```
집계 기준:
- dailyStats[].missionCount: is_review = false만 집계
- totalWeeklyMissions: is_review = false만 집계
```

---

## GET /parent/child/{childId}/privacy-log

> 개인정보 감지 이력 조회

**담당**: BE (문한일) | **인증**: PARENT

**Response 200**

```json
{
  "success": true,
  "data": {
    "totalCount": 5,
    "weeklyCount": 2,
    "events": [
      {
        "detectedType": "NAME",
        "masked": true,
        "detectedAt": "2026-03-29T10:00:00Z"
      },
      {
        "detectedType": "SCHOOL",
        "masked": false,
        "detectedAt": "2026-03-27T15:30:00Z"
      }
    ]
  }
}
```

> ⚠️ 원문 텍스트 절대 반환하지 않음. 감지 유형(detectedType)만 반환.

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 본인 자녀가 아닌 childId | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| childId 없음 | 404 | NOT_FOUND | 자녀 프로필을 찾을 수 없습니다 |

---

## GET /parent/child/{childId}/weak-points

> 자주 틀리는 **미션 분석** (최근 30일)

**담당**: BE (문한일) | **인증**: PARENT

> 🆕 **v1.1 수정**: 기존 설명 "자주 틀린 문제 유형 분석"에서 **"미션 단위 분석"** 으로 정정.

**Response 200**

```json
{
  "success": true,
  "data": {
    "analyzedPeriod": "최근 30일",
    "weakPoints": [
      {
        "missionId": "uuid",
        "missionTitle": "팩트체크란?",
        "stage": 3,
        "incorrectRate": 0.6,
        "attemptCount": 5
      },
      {
        "missionId": "uuid",
        "missionTitle": "개인정보 보호",
        "stage": 2,
        "incorrectRate": 0.4,
        "attemptCount": 3
      }
    ]
  }
}
```

**예외 처리**

| 상황 | HTTP | code | message |
| --- | --- | --- | --- |
| 본인 자녀가 아닌 childId | 403 | FORBIDDEN | 접근 권한이 없습니다 |
| childId 없음 | 404 | NOT_FOUND | 자녀 프로필을 찾을 수 없습니다 |
| 분석 데이터 없음 | 200 | - | weakPoints: [] 빈 배열 반환 |

---

# 📋 전체 엔드포인트 목록

> 🆕 **v1.2 변경**: 신규 API 8개 추가 (총 25개 → 33개)

| 메서드 | 경로 | 설명 | 인증 | 버전 |
| --- | --- | --- | --- | --- |
| POST | /parent/register | 자녀 프로필 생성 | PARENT | v1.0 |
| POST | /child/login | 자녀 코드 로그인 | 없음 | v1.0 |
| PUT | /parent/child/{childId}/regenerate-code | 코드 재발급 | PARENT | v1.0 |
| GET | /missions | 미션 목록 | CHILD | v1.0 |
| **GET** | **/missions/daily** | **오늘의 AI 미션 조회** | CHILD | **v1.2 신규** |
| GET | /missions/{missionId}/questions | 문제 조회 | CHILD | v1.0 |
| POST | /missions/{missionId}/submit | 정답 제출 + 채점 | CHILD | v1.0 |
| GET | /pet | 펫 목록 조회 | CHILD | v1.0 |
| **PUT** | **/pet/equip** | **펫 장착 변경** | CHILD | **v1.2 신규** |
| POST | /chat/send | GPT 챗봇 전송 | CHILD | v1.0 |
| POST | /privacy/event | 개인정보 이벤트 기록 | CHILD | v1.0 |
| POST | /gacha/pull | 가챠 뽑기 | CHILD | v1.0 |
| GET | /gacha/fragments | 조각 현황 | CHILD | v1.0 |
| POST | /gacha/exchange | 조각 교환 | CHILD | v1.0 |
| GET | /streak | 스트릭 현황 | CHILD | v1.0 |
| POST | /streak/partner | 파트너 연결 | CHILD | v1.0 |
| DELETE | /streak/partner | 파트너 해제 | CHILD | v1.0 |
| **GET** | **/streak/milestones** | **목표 마일스톤 조회** | CHILD | **v1.2 신규** |
| **POST** | **/streak/milestone** | **목표 마일스톤 설정** | CHILD | **v1.2 신규** |
| **POST** | **/streak/milestone/claim** | **목표 마일스톤 보상 수령** | CHILD | **v1.2 신규** |
| GET | /quests/daily | 데일리 퀘스트 | CHILD | v1.0 |
| GET | /quests/weekly | 위클리 퀘스트 | CHILD | v1.0 |
| POST | /quests/claim | 퀘스트 보상 수령 (MANUAL만) | CHILD | v1.0 |
| GET | /achievements | 업적 현황 | CHILD | v1.0 |
| GET | /return-reward | 복귀 보상 확인 | CHILD | v1.0 |
| POST | /return-reward/claim | 복귀 보상 수령 | CHILD | v1.0 |
| **GET** | **/parent/children** | **자녀 목록 조회** | PARENT | **v1.2 신규** |
| **DELETE** | **/parent/child/{childId}** | **자녀 프로필 삭제** | PARENT | **v1.2 신규** |
| **POST** | **/parent/fcm-token** | **부모 FCM 토큰 등록/갱신** | PARENT | **v1.2 신규** |
| GET | /parent/child/{childId}/summary | 자녀 요약 | PARENT | v1.0 |
| GET | /parent/child/{childId}/weekly-stats | 주간 통계 | PARENT | v1.0 |
| GET | /parent/child/{childId}/privacy-log | 개인정보 이력 | PARENT | v1.0 |
| GET | /parent/child/{childId}/weak-points | 취약 미션 분석 | PARENT | v1.0 |

---

# 🗄️ ERD 반영 체크리스트

> **BE(한일)에게**: v1.2 구현 시 아래 DB 변경사항을 ERD 설계서에도 반영해주세요!

## v1.1 체크리스트 (완료 확인)

| # | 대상 | 변경 내용 | 반영 여부 |
| --- | --- | --- | --- |
| 1 | child_profiles | `session_version INT NOT NULL DEFAULT 1` 컬럼 추가 | ✅ ERD v1.1 반영됨 |
| 2 | child_profiles | `today_xp_date DATE` 컬럼 추가 | ✅ ERD v1.1 반영됨 |
| 3 | child_profiles | `weekly_xp_week_start DATE` 컬럼 추가 | ✅ ERD v1.1 반영됨 |
| 4 | quiz_attempts | 테이블 신규 생성 | ✅ ERD v1.1 반영됨 |
| 5 | return_reward_claims | 테이블 신규 생성 | ✅ ERD v1.1 반영됨 |
| 6 | pets | `UNIQUE (child_id, pet_type)` 제약 추가 | ✅ ERD v1.1 반영됨 |

## v1.2 체크리스트 (신규 반영 필요)

| # | 대상 | 변경 내용 | 우선순위 |
| --- | --- | --- | --- |
| 7 | parent_accounts | `fcm_token TEXT`, `platform TEXT` 컬럼 추가 | 🔴 필수 |
| 8 | streak_milestones | 테이블 신규 생성 (ERD v1.1에 이미 있음 — 확인 필요) | 🟡 확인 |
| 9 | pets | `mood` 컬럼 DEFAULT 값 `'IDLE'` 확인 (신규 펫 생성 시 IDLE로 시작) | 🟡 확인 |

---

> 📝 **v1.2 작성 기준**: API 명세서 v1.1 + 교차검증 보고서 (2026-04-05) 반영
>
> 🔗 **연관 문서**: ERD 설계서 v1.1 | 기능 명세서 v2.1 | API 명세서 v1.0/v1.1
>
> 📌 **다음 단계**: ERD 체크리스트 #7 반영(parent_accounts 컬럼 추가) → Spring Boot 구현
>
> 📌 **기능명세서 동기화 필요**: "다음 등급 알 해금" 폐기 반영 → 기능명세서 v2.2 예정
