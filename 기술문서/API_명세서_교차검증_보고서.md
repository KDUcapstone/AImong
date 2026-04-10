# AImong API 명세서 교차검증 보고서

**검증 대상**: API 명세서 v1.1 / ERD 설계서 v1.1 / 기능 명세서 v2.1
**검증일**: 2026-04-05
**검증자**: Claude (한욱 요청)

---

## 1. 요약

API 명세서 v1.1은 전반적으로 ERD 설계서, 기능 명세서와 높은 수준의 정합성을 유지하고 있습니다. ERD 체크리스트(#1~#6)에 명시된 변경사항은 ERD v1.1에 모두 반영되어 있으며, 기능 명세서의 핵심 로직도 API에 잘 매핑되어 있습니다. 다만 아래와 같은 **불일치/누락/모호한 부분**이 발견되었습니다.

---

## 2. 심각도 높음 (구현 시 버그 유발 가능)

### 2-1. DELETE /streak/partner 구현 노트가 ERD 구조와 불일치

- **API 명세서** (라인 1181): `friend_streaks에서 (child_a_id = 나 OR child_b_id = 나) 레코드 조회` 라고 기술
- **ERD 설계서**: friend_streaks는 `child_id PK + partner_child_id UNIQUE` 대칭 2행 모델로 설계됨. `child_a_id`, `child_b_id` 컬럼은 존재하지 않음
- **수정 필요**: `child_id = 나` 인 행 + `partner_child_id = 나` 인 행을 모두 삭제하는 방식으로 구현 노트를 수정해야 함 (또는 ON DELETE CASCADE로 한쪽만 삭제해도 자동 정리되는지 확인)

### 2-2. 펫 장착/변경 API 누락

- **기능 명세서** (섹션 5-1): "장착 변경 시 기존 펫 XP 유지 (초기화 없음)", "장착한 펫만 XP 획득" 등의 규칙이 명시됨
- **ERD 설계서**: `equipped_pets` 테이블이 별도로 존재 (child_id PK, pet_id)
- **API 명세서**: `GET /pet`으로 조회만 가능하고, **펫 장착/변경 API가 없음**
- **현재 상태**: "MVP에서는 스타터 가챠 후 첫 번째 펫 자동 장착 (별도 API 없음)"이라고 적혀 있으나, 이후 2번째 펫부터는 장착 변경이 필요함
- **제안**: `PUT /pet/equip` 또는 `POST /pet/equip` API 추가 필요 (또는 MVP 범위에서 제외한다면 명시적으로 기술)

### 2-3. 사용자 목표 마일스톤(streak_milestones) 관련 API 누락

- **기능 명세서** (섹션 10-2): "사용자 목표 마일스톤 (30일 이후, 25일 단위) — 사용자가 직접 목표 설정"
- **ERD 설계서**: `streak_milestones` 테이블이 설계되어 있음 (child_id, target_days, tier, achieved, reward_claimed)
- **API 명세서**: 목표 설정/조회/보상 수령 관련 API가 전혀 없음
- **제안**: 목표 설정 `POST /streak/milestone`, 목표 조회 `GET /streak/milestones`, 보상 수령 `POST /streak/milestone/claim` 등의 API 추가 필요

### 2-4. 퀘스트 보상 정책 상세 누락

- **기능 명세서** (섹션 5-1-1, 5-1-2)에서는 퀘스트별 보상이 명시되어 있지 않고 예시만 존재
- **API 명세서**에서도 각 퀘스트별 구체적 보상이 불명확:
  - `MISSION_1` (AUTO): "별도 수령 없음" → 실제 보상이 뭔지 불명확 (미션 자체 XP만? 추가 보상 없음?)
  - `CHAT_GPT` (AUTO): XP 5 자동 지급 → OK
  - `XP_20` (MANUAL): 일반 티켓 1장 → OK
  - `ALL_3` (MANUAL): 일반 티켓 1장 → OK
  - **위클리 퀘스트 보상**: `XP_100` → 레어 티켓 1장, `MISSION_5` → 일반 티켓 2장, `CHAT_3` → 일반 티켓 1장
- **문제점**: 이 보상 값들이 기능 명세서에는 명시되어 있지 않아, 둘 중 어디가 정본인지 모호

---

## 3. 심각도 중간 (구현 혼란 유발 가능)

### 3-1. mood ENUM 값 불일치

- **ERD 설계서**: `pet_mood_enum AS ENUM ('HAPPY', 'IDLE', 'SAD_LIGHT', 'SAD_DEEP')` — 4가지 상태
- **기능 명세서** (섹션 5-4): `HAPPY / SAD_LIGHT / SAD_DEEP` — 3가지 상태 (IDLE 언급은 있으나 계산식에서는 daysSinceLastMission == 0이면 HAPPY)
- **API 명세서**: `GET /pet` 응답에 `mood` 필드가 없음
- **문제점**:
  1. API에서 mood를 반환하지 않으면 FE는 어떻게 펫 상태를 알 수 있는지? (FE에서 직접 계산하라는 건지, API가 빠진 건지)
  2. ERD의 IDLE 상태와 기능 명세서의 계산 로직이 정확히 일치하지 않음

### 3-2. 가챠 레벨업 보상 정책 상세 부족

- **API 명세서** (라인 948): 레벨업 시 `tickets.normal += 2, shield_count += 1, FCM 비동기 발송`
- **기능 명세서** (섹션 9-2): "구간 레벨업 체크 (이전 횟수 구간 != 현재 구간)" 이라고만 되어 있고 구체적 보상이 없음
- **문제점**: 가챠 레벨업(Lv.1→2, 2→3, 3→4) 시 매번 normal 2장 + shield 1개인지, 구간별로 보상이 다른지 불명확

### 3-3. FCM 알림 — 부모 디바이스 토큰 관리 API 누락

- **기능 명세서** (섹션 8-2): 부모에게 FCM 발송 (개인정보, 미학습, 주간 리포트, 레벨업)
- **API 명세서**: FCM 발송 로직은 privacy/event, 미션 제출 등에서 비동기 처리로 언급되지만, **부모 디바이스의 FCM 토큰을 등록/갱신하는 API가 없음**
- **제안**: `POST /parent/fcm-token` 또는 `PUT /parent/device` 등의 API 추가 필요

### 3-4. 오늘의 AI 미션 관련 API 누락

- **기능 명세서** (섹션 10-3): "오늘의 AI 미션 — 매일 23:30 생성, 해당 날짜 00:00~23:59 완료 가능"
- **ERD 설계서**: `daily_missions`, `daily_mission_questions` 테이블이 설계되어 있음
- **API 명세서**: 오늘의 AI 미션을 조회하는 전용 API가 없음 (`GET /missions`에서 통합 처리인지 별도인지 불명확)
- **제안**: `GET /missions/daily` 또는 기존 `GET /missions` 응답에 오늘의 미션 섹션을 추가하는 방식 명시 필요

### 3-5. 부모 자녀 목록 조회 API 누락

- **기능 명세서** (섹션 2): 부모 1명이 여러 자녀 프로필을 가질 수 있는 구조 (1:N 관계)
- **ERD 설계서**: `child_profiles.parent_id` FK로 1:N 관계 정의됨
- **API 명세서**: 부모가 자신의 자녀 목록을 조회하는 `GET /parent/children` API가 없음 (대시보드 API들은 모두 `childId`를 경로에 필요로 함)
- **제안**: `GET /parent/children` 추가 필요

### 3-6. 자녀 프로필 삭제 API 누락

- **기능 명세서** (섹션 12, EC-04): "부모가 자녀 프로필 삭제 → CASCADE DELETE"
- **ERD 설계서**: CASCADE DELETE 설정됨
- **API 명세서**: `DELETE /parent/child/{childId}` API가 없음

### 3-7. 아이몽 달성 시 "다음 등급 알 해금" 정책 혼란

- **기능 명세서** (섹션 5-3): "아이몽 달성 시 → pet.xp 리셋 + 다음 등급 알 해금"
- **API 명세서** (라인 564): `newEggUnlocked 필드 제거됨 (v1.2) — 섹션 5-3 정책에서 "다음 등급 알 해금" 방식 폐기 확정`
- **문제점**: API v1.2에서 폐기했다고 하는데, 기능 명세서 v2.1에는 여전히 "다음 등급 알 해금"이 남아있음. 기능 명세서도 업데이트 필요

---

## 4. 심각도 낮음 (명확화/개선 권장)

### 4-1. 자녀 홈 화면 데이터 조회 API 미정의

- 기능 명세서 (섹션 13)에서 HomeState에 HAPPY, SAD_LIGHT, SAD_DEEP, PET_EVOLVED 등 다양한 상태를 정의하지만, 홈 화면에 필요한 통합 데이터(펫 상태, 오늘 XP, 스트릭, 퀘스트 진행도 등)를 한번에 조회하는 API가 없음
- 현재는 `/pet`, `/streak`, `/quests/daily`, `/achievements` 등을 개별 호출해야 하는 구조
- 제안: `GET /home` 또는 `GET /child/dashboard` 같은 통합 API 고려

### 4-2. privacy_detected_type_enum 불일치

- **ERD 설계서**: `'NAME', 'SCHOOL', 'AGE', 'PHONE', 'EMAIL', 'ADDRESS', 'DATE', 'URL', 'ETC'` — 9개
- **API 명세서** (라인 807~817): NAME, SCHOOL, AGE, PHONE, EMAIL, ADDRESS, DATE, URL — 8개 (`ETC` 누락)
- **기능 명세서** (섹션 7-2): 감지 대상으로 전화번호, 이메일, 주소, 날짜, URL, 이름, 학교, 나이/학년 언급

### 4-3. 오프라인 재전송(idempotency_key) 처리 API 스펙 부족

- **기능 명세서** (섹션 12, EC-08): "FE Room DB 큐 저장 → 자동 POST"
- **ERD 설계서**: `mission_attempts.idempotency_key TEXT UNIQUE`로 중복 방지
- **API 명세서**: POST /missions/{missionId}/submit에 `idempotency_key` 요청 필드가 없음
  - 구현 노트에서 mission_attempts INSERT 시 idempotency_key를 언급하지만, Request Body에는 없음
  - 헤더(`Idempotency-Key`)로 받는 건지, Body 필드인지 명시 필요

### 4-4. 스트릭 보호권(shieldCount) 획득 경로 불명확

- **기능 명세서** (섹션 10-5): "레벨업 보상으로 지급"
- **API 명세서** (라인 129): "v1.1에서는 획득/조회만 정의. 스트릭 유지 로직에서 자동 소비하지 않음"
- **문제점**: 가챠 레벨업 시 `shield_count += 1`은 있으나, 자정 스케줄러에서 보호권을 소비하는 로직이 API에 없음. 기능 명세서에는 "자정 스케줄러에서 자동 사용" 로직이 있는데 API v1.1에서는 의도적으로 미구현인지 불분명

### 4-5. 복습 모드 XP가 펫에게 적립되는지 불명확

- **API 명세서** (라인 625): `⑧ 펫 성장 단계 체크 (equippedPetId != null && !isReview)` — 펫 성장은 리뷰가 아닐 때만
- 하지만 공통 XP 적립 함수(라인 158~167)에서는 `장착 펫이 있으면 pets.xp += amount`로 되어있어, 복습 시에도 펫 XP가 올라가는 것처럼 보임
- **질문**: 복습 XP가 펫에게도 가는지 아닌지 명시 필요

### 4-6. 위클리 퀘스트 MISSION_5 집계 기준 모호

- `MISSION_5`: 이번 주 미션 5개 완료 — is_review=false만 카운트인지 기능 명세서에 명시 없음
- API 명세서에서는 부모 통계에 대해서만 `is_review=false` 규칙을 명시

---

## 5. ERD 체크리스트 검증 결과

API 명세서 하단의 ERD 반영 체크리스트(#1~#6)가 ERD v1.1에 모두 반영되었는지 확인:

| # | 항목 | ERD 반영 여부 |
|---|------|-------------|
| 1 | child_profiles.session_version | ✅ 반영됨 |
| 2 | child_profiles.today_xp_date | ✅ 반영됨 |
| 3 | child_profiles.weekly_xp_week_start | ✅ 반영됨 |
| 4 | quiz_attempts 테이블 | ✅ 반영됨 (섹션 4-1) |
| 5 | return_reward_claims 테이블 | ✅ 반영됨 (섹션 8-1) |
| 6 | pets UNIQUE(child_id, pet_type) | ✅ 반영됨 |

---

## 6. 전체 엔드포인트 vs 기능 커버리지 매트릭스

| 기능 (기능명세서 기준) | API 커버 여부 | 비고 |
|----------------------|-------------|------|
| 부모 온보딩/자녀 생성 | ✅ POST /parent/register | |
| 자녀 로그인 | ✅ POST /child/login | |
| 코드 재발급 | ✅ PUT /parent/child/{childId}/regenerate-code | |
| 미션 목록 조회 | ✅ GET /missions | |
| 미션 문제 조회 | ✅ GET /missions/{missionId}/questions | |
| 미션 정답 제출 | ✅ POST /missions/{missionId}/submit | |
| 펫 조회 | ✅ GET /pet | |
| **펫 장착 변경** | ❌ **누락** | MVP 이후에도 필요 |
| GPT 챗봇 | ✅ POST /chat/send | |
| 개인정보 이벤트 | ✅ POST /privacy/event | |
| 가챠 뽑기 | ✅ POST /gacha/pull | |
| 조각 조회 | ✅ GET /gacha/fragments | |
| 조각 교환 | ✅ POST /gacha/exchange | |
| 스트릭 조회 | ✅ GET /streak | |
| 파트너 연결 | ✅ POST /streak/partner | |
| 파트너 해제 | ✅ DELETE /streak/partner | |
| **스트릭 목표 설정** | ❌ **누락** | ERD에 테이블 있음 |
| **스트릭 목표 보상 수령** | ❌ **누락** | ERD에 테이블 있음 |
| 데일리 퀘스트 | ✅ GET /quests/daily | |
| 위클리 퀘스트 | ✅ GET /quests/weekly | |
| 퀘스트 보상 수령 | ✅ POST /quests/claim | |
| 업적 조회 | ✅ GET /achievements | |
| 복귀 보상 확인 | ✅ GET /return-reward | |
| 복귀 보상 수령 | ✅ POST /return-reward/claim | |
| 부모 자녀 요약 | ✅ GET /parent/child/{childId}/summary | |
| 부모 주간 통계 | ✅ GET /parent/child/{childId}/weekly-stats | |
| 부모 개인정보 이력 | ✅ GET /parent/child/{childId}/privacy-log | |
| 부모 취약점 분석 | ✅ GET /parent/child/{childId}/weak-points | |
| **부모 자녀 목록 조회** | ❌ **누락** | 대시보드 진입에 필수 |
| **자녀 프로필 삭제** | ❌ **누락** | 기능명세서 EC-04 |
| **부모 FCM 토큰 등록** | ❌ **누락** | FCM 발송 전제조건 |
| **오늘의 AI 미션 조회** | ❌ **누락** | ERD에 테이블 있음 |
| **홈 화면 통합 데이터** | ⚠️ 개선 권장 | 개별 API로 커버 가능 |

---

## 7. 권장 조치 우선순위

### 즉시 수정 (구현 착수 전)

1. `DELETE /streak/partner` 구현 노트의 `child_a_id/child_b_id`를 ERD의 대칭 2행 모델에 맞게 수정
2. `PUT /pet/equip` (펫 장착 변경) API 추가
3. `GET /parent/children` (자녀 목록 조회) API 추가
4. 스트릭 목표 마일스톤 관련 API 추가 (설정/조회/보상수령)

### 구현 초기에 확정 필요

5. `POST /parent/fcm-token` (FCM 토큰 등록) API 추가
6. `DELETE /parent/child/{childId}` (자녀 삭제) API 추가
7. 오늘의 AI 미션 조회 방식 확정 (별도 API vs GET /missions 통합)
8. privacy_detected_type에 `ETC` 포함 여부 통일
9. `idempotency_key` 전달 방식 명시 (헤더 vs Body)
10. 복습 XP의 펫 적립 여부 확정

### 기능 명세서 동기화

11. "다음 등급 알 해금" 정책 폐기를 기능 명세서 v2.1에도 반영
12. 퀘스트별 구체적 보상 값을 기능 명세서에 명시 (정본 명확화)
13. 가챠 레벨업 보상 정책 구체화

---

> 이 보고서는 API 명세서 v1.1, ERD 설계서 v1.1, 기능 명세서 v2.1을 기반으로 교차 검증한 결과입니다. 구현 착수 전에 팀원들과 위 항목들을 논의하여 확정하는 것을 권장합니다.
