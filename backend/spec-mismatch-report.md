# AImong 명세/구현 불일치 보고서

작성일: 2026-05-12

대상 문서:
- `private-docs/📄 AImong 기능 명세서 v3 4 32dbca9939c88152808fd6ee490d0bfa.md`
- `private-docs/🗄️ AImong ERD 설계서 v2 2 332bca9939c881198522c57d84bac149.md`
- `private-docs/🔌 AImong API 명세서 v2 8/`

## 요약

현재 구현에서 `setId`는 API 호환, 진행도, 복습, submit/report 연결을 위한 키로 유지되고 있다.

하지만 실제 문항 선택 기준은 `question_bank.set_id`가 아니다. 현재 구현은 `setId`로 `mission_set`을 찾은 뒤, 해당 세트의 `missionId`와 `starLevel`을 사용해 `question_bank`에서 `missionId + difficulty` 기준으로 문항 풀을 조회하고, 별 난이도 비율에 맞춰 10문항을 런타임 재구성한다.

즉 현재 구현 기준에서 `setId`는 "고정 10문항 세트 ID"가 아니라 "missionId + starLevel 진행도/API 호환용 학습 단위"에 가깝다. 실제 10문항 고정은 `quiz_attempts.question_ids_json`과 `attemptId`가 담당한다.

## 1. 출제 기준 불일치

문서:
- `setId`를 무시하지 않고 해당 세트 기준으로 문항을 조회한다고 설명한다.
- `question_bank_safe`에서 `set_id` 기준 활성 문항 10개를 조회한다고 설명한다.
- 일부 API 문서는 `setId`에 사전 배정된 10문항을 제공한다고 설명한다.

구현:
- `setId`는 `mission_set` 조회와 attempt/progress 연결에 사용된다.
- 실제 문항 조회는 `missionId + difficulty` 기준이다.
- `question_bank.set_id`는 출제 조건으로 사용하지 않는다.

현재 구현 흐름:
1. `setId` 또는 `missionId + starLevel`로 `mission_set` 결정
2. `missionSet.missionId`, `missionSet.starLevel` 확인
3. `question_bank`에서 `missionId + difficulty` 기준 문항 풀 조회
4. `starLevel` 비율대로 안 푼 문제 우선 10문항 선택
5. 선택된 문항은 `quiz_attempts.question_ids_json`에 저장

## 2. Check 검증 설명 불일치

문서:
- `setId`와 `questionId`가 같은 세트에 속하는지 검증한다고 설명한다.

구현:
- 진행 중인 `quiz_attempt`를 `childId + setId + status=IN_PROGRESS`로 찾는다.
- `questionId`가 해당 attempt의 `question_ids_json` 안에 있는지 검증한다.
- 추가로 `questionId`가 같은 `missionId`에 속하는 활성 문항인지 확인한다.
- `question_bank.set_id = setId` 검증은 수행하지 않는다.

현재 정책상 더 정확한 문구:
- `setId`는 진행 중 attempt를 찾기 위한 키다.
- 문항 소속 검증은 `quiz_attempts.question_ids_json` 포함 여부를 기준으로 한다.

## 3. API 예시 ID 타입 불일치

문서:
- 예시에 숫자형 ID가 남아 있다.
- 예: `missionId: 1`, `setId: 37`, `questionId: 1001`, `attemptId: 501`
- Java 예시에도 `Long setId`, `Long missionId`가 남아 있다.

구현:
- `missionId`: UUID
- `questionId`: UUID
- `attemptId`: UUID
- `setId`: 문자열

문서 예시는 UUID/문자열 기준으로 바뀌어야 한다.

예:
```json
{
  "missionId": "86157cba-318c-3d6c-ab8a-580b1323ff7c",
  "setId": "S0101-L1",
  "attemptId": "8c9d0e1f-1111-2222-3333-444444444444",
  "questionId": "4b8f6f0e-1111-2222-3333-555555555555"
}
```

## 4. 문항 신고 API 경로 불일치

문서 일부:
- `POST /missions/{missionId}/questions/{questionId}/report`

처리 후 구현:
- `POST /api/missions/{missionId}/questions/{questionId}/report`

현재 구현 기준 정본 경로:
```text
POST /api/missions/{missionId}/questions/{questionId}/report
```

## 5. ERD `mission_answer_results.selected_answer` 누락

문서:
- ERD v2.2의 `mission_answer_results` DDL에는 `selected_answer` 컬럼이 없다.

구현:
- `mission_answer_results.selected_answer` 컬럼이 존재한다.
- submit 시 사용자가 제출한 답을 저장한다.
- report 응답의 `submittedAnswer`를 구성하는 데 사용한다.

현재 구현 기준으로 ERD에 포함되어야 하는 컬럼:
```sql
selected_answer TEXT
```

## 참고: 보상 coin

API 문서의 `rewards.coin: 30`은 구현을 명세에 맞추는 방향으로 처리 대상이다. 문서 수정 대상에서 제외한다.
## 6. `GET /missions` 학습맵 구조 불일치

문서:
- API 통합 문서 v2.8 변경 요약은 `GET /missions` 응답을 `levels[].stages[].sets[]` 기준으로 설명한다.

구현:
- 현재 구현과 미션 API 하위 문서는 `stages[].missions[].starLevels[]` 구조다.
- 세트별 상세 목록을 직접 펼치지 않고, 소단원별 별 난이도 진행도만 반환한다.

현재 상태:
- 구현은 미션 API 하위 문서와는 맞지만, API 통합 문서 변경 요약의 `levels[].stages[].sets[]` 표현과 불일치한다.

## 7. 문제 조회 `energyCost` 예시 불일치

문서:
- 미션 API 문제 조회 예시는 `energyCost: 1`, `energyBefore: 5`, `energyAfter: 4`로 되어 있다.

구현:
- 기능 명세 v3.4와 구현은 일반 모드 학습 세트 시작 시 에너지 5를 차감한다.
- `ChildProfile.MISSION_ENERGY_COST = 5`이며 문제 조회 응답의 `energyCost`도 5 기준이다.

현재 상태:
- 구현은 기능 명세와 맞다.
- API 예시의 `energyCost`/`energyAfter` 값이 5 차감 기준으로 맞지 않는다.

## 8. `score` 의미 불일치

문서:
- API 명세 submit/report 예시는 `score: 80`, `correctCount: 8`처럼 `score`를 100점 환산 점수로 표현한다.
- 기능 명세 일부 예시는 `score: 8`처럼 정답 개수 의미로 표현한다.

구현:
- submit 응답은 이미 100점 환산 점수를 반환한다.
- report 응답도 API 명세 기준에 맞춰 100점 환산 점수를 반환하도록 수정했다.
- 내부 저장값 `mission_attempts.score`는 정답 개수이며, 외부 응답에서만 환산한다.

현재 기준:
- 외부 API 응답의 `score`는 API 명세 기준인 100점 환산 점수로 통일한다.
- 정답 개수는 `correctCount`로 표현한다.

## 9. submit `rewards` 구조 불일치

문서:
- 기능 명세 일부 submit 예시는 `rewards: []` 배열로 되어 있다.
- API 명세 submit/report 예시는 `rewards: { coin, exp, fragments }` 객체다.

구현:
- 현재 구현은 API 명세 기준으로 `rewards.coin`, `rewards.exp`, `rewards.fragments` 객체를 반환한다.

현재 기준:
- API 명세 기준의 객체 구조를 정본으로 본다.
- 기능 명세의 `rewards: []` 예시는 API 명세 기준과 맞지 않는다.
