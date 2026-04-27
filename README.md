# AImong

초등학생 대상 AI 리터러시 교육을 게임처럼 풀어낸 Android 앱 프로젝트입니다.  
자녀는 6자리 코드로 학습, 퀘스트, 챗봇, 가챠, 스트릭 기능을 이용하고, 부모는 Google 로그인 기반으로 자녀 현황과 리포트를 확인하는 구조를 목표로 합니다.

이 README는 팀원이 저장소를 처음 열었을 때 빠르게 아래를 파악할 수 있도록 작성했습니다.

- 프로젝트가 무엇인지
- 현재 저장소 구조가 어떻게 나뉘는지
- 어떤 문서를 먼저 읽어야 하는지
- 로컬에서 무엇을 준비해야 하는지
- 현재 구현 상태에서 무엇을 주의해야 하는지

## 1. 프로젝트 한눈에 보기

| 항목 | 내용 |
| --- | --- |
| 앱 이름 | AImong (아이몽) |
| 주요 사용자 | 초등학생 8~13세 자녀, 학부모 |
| 핵심 목적 | AI 리터러시 교육 x 게임화 |
| 클라이언트 | Android 앱 |
| 서버 | Spring Boot API 서버 |
| 데이터 저장소 | Supabase PostgreSQL |
| 인증 | 부모: Firebase Auth, 자녀: 서버 세션 토큰 |
| 외부 연동 | OpenAI API, Firebase Auth, FCM, Google ML Kit |

### 핵심 기능 축

- 오늘의 AI 미션 풀이와 XP 적립
- 펫 성장과 상태 변화
- 데일리/위클리 퀘스트
- GPT 챗봇
- 개인정보 감지 및 보호 UX
- 가챠/조각/티켓 시스템
- 부모 대시보드 및 자녀 학습 통계
- 공동 스트릭

## 2. 저장소 구조

```text
AImong/
├─ android/   # Android 앱 (Kotlin, 단일 :app 모듈)
├─ backend/   # Spring Boot API 서버
├─ prompts/   # AI 코딩 컨텍스트 문서
└─ README.md
```

### `android/`

Android 앱 프로젝트입니다.

- 빌드 시스템: Gradle Kotlin DSL
- 현재 구조: `core` + `feature/*`
- 기능 패키지: `auth`, `home`, `mission`, `chat`, `gacha`, `streak`, `quest`, `parent`
- 네비게이션: `res/navigation/nav_child.xml`, `res/navigation/nav_parent.xml`

대략적인 구조:

```text
android/app/src/main/java/com/aimong/android/
├─ core/         # 공통 모듈 (network, local, ui, privacy, fcm, util)
├─ feature/      # 기능별 data / domain / presentation
├─ navigation/   # 네비게이션 관련 코드
├─ AimongApp.kt
└─ MainActivity.kt
```

### `backend/`

Spring Boot 백엔드 프로젝트입니다.

- 패키지 기준: `domain / global / infra`
- 도메인 패키지: `auth`, `chat`, `gacha`, `mission`, `parent`, `pet`, `privacy`, `quest`, `reward`, `streak`
- 공통 패키지: `config`, `exception`, `filter`, `response`, `scheduler`, `util`

대략적인 구조:

```text
backend/src/main/java/com/aimong/backend/
├─ domain/   # 기능 도메인별 controller/service/repository/entity/dto
├─ global/   # 전역 설정, 예외, 필터, 응답, 스케줄러, 유틸
├─ infra/    # 외부 연동 (fcm, openai, supabase)
└─ BackendApplication.java
```

### `prompts/`

AI 코딩 도구용 참고 문서입니다.

- `context-fe.md`: Android 구조/규칙 요약
- `context-be.md`: Backend 구조/규칙 요약

현재 파일 인코딩 이슈로 터미널 출력이 깨져 보일 수 있으니, 에디터에서 UTF-8로 확인하는 것을 권장합니다.

## 3. 문서 우선순위

실제 구현 전에 아래 문서를 먼저 읽는 것을 권장합니다.

### 저장소 바깥 기술 문서

- 기능 명세서: `C:\Users\hwkim\Desktop\univ\4-1\캡스톤\기술문서\기능명세서.zip`
- API 명세서: `C:\Users\hwkim\Desktop\univ\4-1\캡스톤\기술문서\API명세서.zip`
- ERD 설계서: `C:\Users\hwkim\Desktop\univ\4-1\캡스톤\기술문서\ERD설계서.zip`

### 문서 기준 최신 버전

- 기능 명세서 v2.1
- API 명세서 v1.1
- ERD 설계서 v1.1

### 역할별 추천 읽기 순서

- BE: 기능 명세의 인증 -> 퀘스트 -> 펫 -> 챗봇 -> 개인정보 -> 부모 리포트 -> 가챠 -> 스트릭 -> 연쇄 이벤트 -> 엣지케이스
- FE: 기능 명세의 사용자 -> 인증 -> 퀘스트 -> 펫 -> 챗봇 -> 개인정보 -> 가챠 -> 화면 상태
- 공통: API 명세 공통 규칙, 에러 규격, 동시성 제어 대상 API
- 공통: ERD 설계 원칙, 테이블 목록, ENUM 정의

## 4. 기술 스택

## Android

- Kotlin
- Android Gradle Plugin 8.3.2
- minSdk 26 / targetSdk 35 / compileSdk 35
- Hilt
- Retrofit + OkHttp
- Room
- DataStore
- WorkManager
- Firebase Auth / Firebase Messaging
- Google ML Kit Entity Extraction
- Lottie

## Backend

- Java toolchain 21
- Spring Boot 3.5.13
- Spring Web
- Spring Data JPA
- Spring Security
- Validation
- PostgreSQL
- Lombok

## External Services

- Supabase PostgreSQL
- Firebase Auth / Firebase Admin SDK / FCM
- OpenAI API

### 스펙과 코드 차이 메모

- 기능 명세서에는 백엔드 스택이 Java 17로 적혀 있지만, 현재 저장소의 `backend/build.gradle`은 Java 21 toolchain으로 설정되어 있습니다.
- 기능 명세서에는 OpenAI 모델이 `gpt-5-mini`로 적혀 있지만, 현재 코드 주석에는 `gpt-4o-mini`가 보입니다.

작업 시작 전에는 "명세를 따를지, 현재 코드 설정을 따를지"를 한 번 정리하고 들어가는 것을 권장합니다.

## 5. 빠른 시작

## 공통 준비

아래 도구가 필요합니다.

- JDK 21
- Android Studio
- Gradle Wrapper 사용 가능 환경
- Git

또한 아래 외부 서비스 관련 키/설정이 필요할 수 있습니다.

- Firebase 프로젝트 설정
- `google-services.json`
- Supabase 접속 정보
- OpenAI API 키
- FCM 서버 설정

## Android 실행

```bash
cd android
./gradlew.bat assembleDebug
```

권장 실행 방법:

1. Android Studio에서 `android/` 폴더를 연다.
2. Gradle Sync를 완료한다.
3. `app` 모듈을 선택해 에뮬레이터 또는 실기기에서 실행한다.

확인 포인트:

- `google-services.json`은 `.gitignore` 대상이라 별도로 받아서 `android/app/` 또는 프로젝트 요구 위치에 배치해야 합니다.
- Firebase/ML Kit/FCM 관련 초기화 코드는 아직 TODO 주석이 남아 있는 부분이 있습니다.

## Backend 실행

```bash
cd backend
./gradlew.bat bootRun
```

테스트 실행:

```bash
cd backend
./gradlew.bat test
```

확인 포인트:

- 현재 `application.yaml`에는 `spring.application.name` 정도만 들어 있고, 실제 DB/API/Firebase 설정은 추가가 필요합니다.
- 로컬 실행 시 환경별 설정 파일 또는 환경 변수 체계를 먼저 정하는 것이 좋습니다.

## 6. API / 인증 / DB 핵심 요약

## Base URL

```text
https://api.aimong.app/api
```

## 인증 방식

| 대상 | 방식 | 헤더 |
| --- | --- | --- |
| 부모 | Firebase Auth ID Token | `Authorization: Bearer {firebase_id_token}` |
| 자녀 | 서버 발급 세션 토큰 | `Authorization: Bearer {child_session_token}` |

## 공통 응답 형식

```json
{ "success": true, "data": { } }
```

```json
{ "success": false, "error": { "code": "ERROR_CODE", "message": "..." } }
```

## 전역 구현 규칙

- 모든 날짜 계산은 KST(UTC+9), `Asia/Seoul` 기준
- XP 계산은 `Math.floor`
- 정답/해설은 문제 조회 응답에 포함 금지
- 가챠 난수는 서버 사이드 생성
- 채점은 서버에서만 수행
- 개인정보 원문은 서버 DB 저장 금지

## 주요 도메인 / 테이블

| 영역 | 주요 테이블 |
| --- | --- |
| 계정/인증 | `parent_accounts`, `child_profiles` |
| 펫 | `pets`, `equipped_pets` |
| 미션 | `missions`, `question_bank`, `daily_missions`, `daily_mission_questions`, `mission_attempts`, `mission_daily_progress` |
| 퀘스트/업적 | `daily_quests`, `weekly_quests`, `achievements` |
| 스트릭 | `streak_records`, `friend_streaks`, `milestone_rewards` |
| 가챠 | `tickets`, `gacha_pulls`, `fragments` |
| 보호 기능 | `privacy_events`, `chat_usage` |

## 7. 협업 가이드

## 브랜치 / 작업 방식

- 큰 기능 작업 전에는 관련 명세 버전을 먼저 확인합니다.
- FE/BE 모두 명세와 실제 코드가 다를 수 있으니, PR 설명에 "명세 기준"과 "코드 기준"을 함께 적는 것을 권장합니다.
- 동시성/보상/리셋 로직은 API 명세의 전역 규칙과 변경 이력부터 먼저 확인합니다.

## 추천 확인 순서

1. README로 저장소 구조 파악
2. 기능 명세로 제품 흐름 파악
3. API 명세로 요청/응답/예외 확인
4. ERD 명세로 테이블/상태값 확인
5. 실제 코드에서 해당 패키지 진입

## 주요 진입점

- Android 앱 시작점: `android/app/src/main/java/com/aimong/android/MainActivity.kt`
- Android 앱 클래스: `android/app/src/main/java/com/aimong/android/AimongApp.kt`
- Backend 시작점: `backend/src/main/java/com/aimong/backend/BackendApplication.java`
- Backend 설정 파일: `backend/src/main/resources/application.yaml`

## 8. 현재 상태에서 꼭 알아둘 점

- 현재 저장소는 전체 구조와 패키지 틀은 잘 잡혀 있지만, 구현 본문이 비어 있거나 TODO 주석인 파일이 많습니다.
- Android는 단일 `:app` 모듈이며, 기능별 패키지 분리는 되어 있지만 Gradle 모듈 분리는 아직 아닙니다.
- Backend 테스트는 현재 `contextLoads()` 수준의 기본 테스트 1개만 존재합니다.
- Android `src/test`, `src/androidTest`에는 현재 테스트 파일이 없습니다.
- `prompts/` 문서는 협업 보조용이므로, 실제 구현 기준은 반드시 명세서와 코드 둘 다 확인해야 합니다.

## 9. 보안 / 설정 주의사항

`.gitignore` 기준으로 아래 항목은 저장소에 올리지 않습니다.

- `local.properties`
- `google-services.json`
- `keystore/`
- `*.jar`, `*.war`
- `.env`
- `**/application-local.yml`
- `**/application-prod.yml`
- `**/serviceAccountKey.json`

민감 정보는 README 대신 별도 보안 채널 또는 팀 문서에서 공유하세요.

## 10. 체크리스트

새 팀원이 처음 시작할 때는 아래 순서로 확인하면 됩니다.

- 저장소 클론 후 `android/`, `backend/`가 각각 독립 빌드 프로젝트라는 점 이해하기
- 기술문서 3종 최신 버전 확인하기
- Android Studio에서 `android/` 열기
- 백엔드 로컬 설정값 정리하기
- Firebase / Supabase / OpenAI 관련 키 수급하기
- 명세와 현재 코드 차이 메모 남기기
- 담당 기능 패키지 진입 후 TODO 범위 확인하기

---

필요하면 다음 단계로 아래 문서도 이어서 추가할 수 있습니다.

- `docs/onboarding.md`: 팀원 체크리스트 확장판
- `docs/conventions.md`: FE/BE 공통 구현 규칙
- `docs/run-local.md`: 로컬 실행 상세 가이드

