# AImong 명세 수정/추가 필요 항목

기준 문서:

- `private-docs/📄 AImong 기능 명세서 v3 7 32dbca9939c88152808fd6ee490d0bfa.md`
- `private-docs/🗄️ AImong ERD 설계서 v2 4 332bca9939c881198522c57d84bac149.md`
- `private-docs/🔌 AImong API 명세서 v3 1/*`

삭제된 기존 보고서는 참고하지 않았다.

## 1. `currency_transaction_reason_enum`에 `MISSION_CLEAR` 추가 필요

### 현재 문서 상태

기능 명세와 ERD의 톱니바퀴 원장 사유 목록은 아래 값만 허용한다.

- `HEART_REVIVE`
- `STREAK_SHIELD_PURCHASE`
- `QUEST_REWARD`
- `ACHIEVEMENT_REWARD`
- `RETURN_REWARD`
- `ADMIN_ADJUST`

관련 위치:

- 기능 명세 v3.7: 톱니바퀴 원장 reason 목록
- ERD v2.4: `currency_transaction_reason_enum`

### 문서상 문제

미션 퀴즈 명세는 일반 모드 미션 클리어 보상으로 `rewards.gear = 30`을 정본으로 둔다.

또한 기능 명세와 ERD는 모든 톱니바퀴 증감을 `currency_transactions`에 기록한다고 명시한다. 따라서 미션 클리어 보상 지급도 원장 reason이 필요하지만, 현재 reason 목록에는 이를 표현할 값이 없다.

### 수정 제안

`currency_transaction_reason_enum`에 아래 값을 추가한다.

```sql
'MISSION_CLEAR'
```

권장 enum 목록:

```sql
CREATE TYPE currency_transaction_reason_enum AS ENUM (
  'HEART_REVIVE',
  'STREAK_SHIELD_PURCHASE',
  'MISSION_CLEAR',
  'QUEST_REWARD',
  'ACHIEVEMENT_REWARD',
  'RETURN_REWARD',
  'ADMIN_ADJUST'
);
```

기능 명세의 원장 reason 설명도 동일하게 갱신한다.

권장 설명:

- 미션 클리어 보상은 `amount = +30`, reason=`MISSION_CLEAR`, ref_type=`MISSION_ATTEMPT`, ref_id=`attemptId`로 기록한다.

## 2. 펫 API 문서의 `pet_fragments` PK 설명 수정 필요

### 현재 문서 상태

ERD v2.4는 `pet_fragments`를 아래 구조로 정의한다.

```sql
CREATE TABLE pet_fragments (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  child_id     UUID NOT NULL REFERENCES child_profiles(child_id) ON DELETE CASCADE,
  grade        pet_grade_enum NOT NULL,
  count        INT NOT NULL DEFAULT 0 CHECK (count >= 0),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (child_id, grade)
);
```

하지만 펫 API 문서에는 아래처럼 과거 설명이 남아 있다.

```java
// pet_fragments 테이블 (PK: child_id + grade)
```

관련 위치:

- `private-docs/🔌 AImong API 명세서 v3 1/🐣 펫 API 343bca9939c88158beb0c6bf573a39e5.md`

### 문서상 문제

ERD 정본은 `id UUID PRIMARY KEY`와 `UNIQUE(child_id, grade)` 조합이다. 펫 API 문서의 “PK: child_id + grade” 설명은 ERD와 충돌한다.

### 수정 제안

펫 API 문서의 설명을 아래처럼 수정한다.

```java
// pet_fragments 테이블 (PK: id, UNIQUE: child_id + grade)
```

예시 엔티티 코드 블록도 `id` 단일 PK와 `UNIQUE(child_id, grade)` 구조를 기준으로 정리한다.

## 3. 미션 클리어 gear 보상의 원장 기록 규칙 추가 권장

### 현재 문서 상태

미션 퀴즈 API는 submit/report 응답의 `rewards`를 `{ gear, exp, fragments }` 객체로 정의하고, 일반 모드 클리어 예시에서 `gear: 30`을 반환한다.

재화/ERD 문서는 하트 회복과 스트릭 보호권 구매의 원장 기록 규칙은 구체적으로 적고 있다.

### 문서상 부족한 점

`gear: 30` 미션 클리어 보상이 어느 원장 reason/ref로 기록되는지 명시가 없다. “모든 증감은 currency_transactions에 기록한다”는 원칙과 연결되는 세부 규칙이 필요하다.

### 추가 제안

ERD v2.4의 `currency_transactions` 설명 또는 미션 퀴즈 API의 제출 처리 순서에 아래 규칙을 추가한다.

```text
일반 모드 미션 클리어 보상은 currency_transactions에 기록한다.
- amount: +30
- reason: MISSION_CLEAR
- ref_type: MISSION_ATTEMPT
- ref_id: attemptId
```

복습 모드, 불합격, 중도 포기, 만료 attempt는 `gear = 0`이며 원장 지급 기록을 만들지 않는다고 함께 명시하면 좋다.

