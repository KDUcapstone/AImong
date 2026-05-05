# PR/커밋 정리

## 커밋 메시지

### 1. 부모 대시보드 API 구현

```text
feat: 부모 대시보드 API 구현

- 부모 대시보드 summary, weekly-stats, privacy-log, weak-points API 추가
- 부모 인증 principal 기준으로 자녀 소유권 검증 처리
- summary에서 XP, 스트릭, 쉴드, 주간/전체 미션 완료 수, 마지막 활동 시각 반환
- weekly-stats에서 KST 기준 이번 주 월~일 7일 통계와 총 XP/미션 수 집계
- privacy-log에서 개인정보 감지 이벤트 페이지네이션과 주간 감지 수 반환
- weak-points에서 최근 30일 mission_answer_results 기반 미션별 오답률 집계
- 부모 대시보드 응답 DTO 및 통계 조회용 repository 쿼리 추가
```

### 2. 문제은행 교체 및 문항 생성 품질 검증 강화

```text
feat: 문제은행 교체 및 문항 생성 품질 검증 강화

- 도입문이 제거된 문제은행 세트를 메인 데이터로 교체하는 V5 Flyway 마이그레이션 추가
- 문제은행 SQL export 시 PostgreSQL dollar quoting을 사용해 한글/특수문자 포함 문항을 안전하게 출력하도록 수정
- 생성/저장/검증 경로에서 문제 앞 불필요한 도입문을 제거하는 QuestionPromptSanitizer 적용
- dev 문항 생성 응답의 difficulty를 LOW/MEDIUM/HIGH 기준으로 맞추고 numericDifficulty를 별도 반환
- /dev/missions/generate에서 rejected 후보의 repair hint를 반영해 1회 재생성하도록 개선
- OpenAI 문항 생성 프롬프트에 문항 유형별 스키마, zero-based answer, 자연스러운 빈칸, 그럴듯한 오답 규칙 추가
- FILL, OX, 객관식/상황형 문항의 품질 검증을 강화해 비문, 정답 노출, 정답 번호 불일치, 지문-정답 모순, 허수아비 오답을 hard fail 처리
- OpenAI 호출 실패와 정적 리소스 미매핑 예외를 더 명확한 에러로 처리
- 문제은행 exporter 및 문항 품질 검증 회귀 테스트 추가
```

## PR 제목

```text
feat: 부모 대시보드 API 구현 및 문제은행 생성 품질 개선
```

## PR 본문

### 개요

이번 PR은 명세 기준으로 누락되어 있던 부모 대시보드 API를 구현하고, 문제은행 데이터와 AI 문항 생성 품질을 함께 정리합니다.

부모 대시보드는 자녀별 학습 현황, 주간 통계, 개인정보 감지 로그, 취약 미션을 조회할 수 있도록 API와 집계 쿼리를 추가했습니다. 문제은행 쪽은 기존 문항 앞에 붙던 불필요한 도입문을 제거한 세트를 DB 마이그레이션으로 반영하고, 신규 문항 생성 시에도 같은 스타일과 품질 기준을 따르도록 프롬프트와 검증 로직을 강화했습니다.

### 부모 대시보드 API

- 부모 인증 사용자를 기준으로 자녀 소유권을 검증합니다.
- 자녀 요약 API에서 다음 정보를 반환합니다.
  - 총 XP
  - 현재 스트릭
  - 쉴드 개수
  - 이번 주 완료 미션 수
  - 전체 완료 미션 수
  - 마지막 활동 시각
- 주간 통계 API에서 KST 기준 이번 주 월요일부터 일요일까지 7일 통계를 반환합니다.
  - 일자별 획득 XP
  - 일자별 완료 미션 수
  - 주간 총 XP
  - 주간 총 완료 미션 수
- 개인정보 로그 API에서 개인정보 감지 이벤트를 페이지네이션으로 조회합니다.
  - 감지 시각
  - 감지 유형
  - 미션 정보
  - 주간 감지 수
- 취약점 API에서 최근 30일 답안 결과를 기준으로 미션별 오답률을 집계합니다.
  - 시도 수
  - 오답 수
  - 오답률
  - 취약 미션 목록
- 부모 대시보드 응답 DTO와 통계 조회용 repository 쿼리를 추가했습니다.

### 문제은행 데이터 교체

- 기존 문제은행 문항 중 다음과 같은 도입문을 제거한 버전을 메인 세트로 사용하도록 변경했습니다.
  - `수업 활동 속 예를 떠올리며`
  - `활동 장면을 떠올리며`
  - `모둠 토의 중이라고 생각하며`
  - `모둠 토의에서 나온 예를 떠올리며`
- V5 Flyway 마이그레이션으로 기존 question_bank 데이터를 정리된 문제 세트로 교체합니다.
- PostgreSQL 문자열 파싱 오류를 피하기 위해 문제은행 SQL exporter가 dollar quoting을 사용하도록 수정했습니다.
- 생성된 SQL에 한글, 따옴표, 특수문자가 포함되어도 Flyway에서 안정적으로 실행되도록 테스트를 보강했습니다.

### 문항 생성 응답 정리

- dev 생성 API 응답을 문제은행 난이도 체계에 맞췄습니다.
  - `difficulty`: `LOW`, `MEDIUM`, `HIGH`
  - `difficultyBand`: `LOW`, `MEDIUM`, `HIGH`
  - `numericDifficulty`: 기존 숫자 난이도
- 신규 생성 문항 저장 시에도 검증/정규화된 질문 문구를 사용합니다.
- `QuestionPromptSanitizer`를 추가해 생성 문항 앞의 불필요한 활동 도입문을 제거합니다.

### 생성 프롬프트 강화

- 문항은 바로 핵심 질문으로 시작하도록 지시했습니다.
- 문제은행 스타일에 맞게 짧고 자연스러운 학생 대상 문장을 생성하도록 강화했습니다.
- 유형별 생성 규칙을 명확히 했습니다.
  - OX: options는 null, answer는 boolean
  - MULTIPLE: 선택지 4개, answer는 0~3 정수
  - FILL: 자연스러운 빈칸 1개, 선택지 4~5개, answer는 `[0]` 형태
  - SITUATION: 선택지 2~4개, answer는 정수
- answer는 zero-based index로 생성하도록 명시했습니다.
- 해설에 `정답은 2번입니다`처럼 화면 표시 번호와 내부 index가 충돌할 수 있는 표현을 피하도록 했습니다.
- 고난도 문제에서는 너무 뻔한 오답을 쓰지 않도록 했습니다.
- 객관식 오답은 말이 안 되는 선택지가 아니라 실제 학생이 헷갈릴 수 있는 그럴듯한 오개념으로 만들도록 지시했습니다.

### 품질 검증 강화

- FILL 문항 검증을 강화했습니다.
  - quoted blank 또는 메타 예시 형태 hard fail
  - 빈칸에 정답을 넣었을 때 비문이 되는 문장 hard fail
  - 정답 대부분이 지문에 이미 노출된 문항 hard fail
  - FILL answer가 `[0]` 같은 단일 index 배열이 아니면 schema fail
- OX 문항 검증을 강화했습니다.
  - `3개로 요청하면 3개 목록을 만들어 준다`처럼 조건을 그대로 반복하는 문항 hard fail
- 선택지 품질 검증을 강화했습니다.
  - 해설의 `정답은 N번`과 실제 answer index가 불일치하면 hard fail
  - 지문은 `50자`인데 정답 선택지가 `200자`처럼 구체 수치가 모순되면 hard fail
  - HIGH 난이도에서 정답은 너무 쉽고 오답은 일부러 틀린 수준이면 hard fail
  - 앱이 일부러 틀린다, 전기를 아낀다, 기분을 읽는다 같은 비현실적 허수아비 오답이 2개 이상이면 hard fail
- schema 실패 시 repair hint를 더 구체적으로 제공하도록 보강했습니다.

### Dev 생성 API 개선

- `/dev/missions/generate`에서 첫 생성 후보가 검증에 실패하면 repair hint를 반영해 한 번 더 생성합니다.
- retry 시 기존 accepted 문항과 기존 문제은행 prompt를 함께 전달해 중복 생성을 줄입니다.
- rejected 후보의 hard fail, warning, repair hint를 응답으로 유지해 품질 디버깅이 가능하도록 했습니다.
- dev API 테스트 화면의 contract check가 변경된 difficulty 응답 구조를 확인하도록 맞췄습니다.

### 예외 처리 보강

- OpenAI structured response 호출 실패 시 응답 본문/상태를 로깅 가능한 AimongException으로 감싸도록 개선했습니다.
- 존재하지 않는 정적 리소스 요청이 내부 서버 오류로 보이지 않도록 `NoResourceFoundException`을 404로 처리했습니다.
- 서버에서 예상하지 못한 예외는 requestId와 함께 로그에 남도록 보강했습니다.

### 테스트

- `./gradlew.bat test` 통과
- 추가/보강된 테스트 범위:
  - 문제은행 SQL exporter dollar quoting
  - 생성 문항 도입문 제거 sanitizer
  - FILL quoted blank/meta blank reject
  - FILL 정답 노출 reject
  - OX 조건 반복형 문항 reject
  - 해설 정답 번호와 answer index 불일치 reject
  - 지문 수치와 정답 선택지 수치 모순 reject
  - FILL 빈칸 비문 reject
  - HIGH 난이도 뻔한 선택지 reject
  - 비현실적 허수아비 오답 reject

### 확인 필요

- V5 마이그레이션은 기존 문제은행 데이터를 정리된 세트로 교체하므로, 배포 전 운영 DB 적용 타이밍 확인이 필요합니다.
- dev 생성 API는 `local`, `dev` 프로필에서만 노출되는 방향으로 운영 profile 설정을 확인해야 합니다.
