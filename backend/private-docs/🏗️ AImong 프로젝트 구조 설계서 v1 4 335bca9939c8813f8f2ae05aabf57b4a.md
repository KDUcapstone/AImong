# 🏗️ AImong 프로젝트 구조 설계서 v1.4

생성자: 한욱 
카테고리: 기술 문서
최종 업데이트 시간: April 11, 2026 11:13 PM
문서 종류: 시스템 설계
허브 분류: 구현 문서

> **버전**: v1.4 | **작성일**: 2026-04-11 | **기반**: 기능 명세서 v2.3 + API 명세서 v1.3 + ERD v1.3
> 

> 🤖 **이 문서는 AI 코딩 도구(Cursor, Copilot 등)가 참고하기 쉽도록 작성되었습니다.**
> 

---

# 📐 설계 원칙

| 항목 | 결정 사항 |
| --- | --- |
| **BE 패키지 구조** | 혼합형 — 도메인별 패키지 안에 controller/service/repository 계층 구분 |
| **FE 아키텍처** | MVVM + Clean Architecture (Presentation → Domain → Data 3레이어) |
| **BE 프레임워크** | Spring Boot 3.x (Java 17) |
| **FE 언어** | Kotlin (Android minSdk 26) |
| **DB** | Supabase (PostgreSQL) — Spring Data JPA + Supabase REST |
| **인증** | Firebase Auth (Google 로그인) + 서버 발급 세션 토큰 (자녀) |
| **푸시** | FCM — Spring Boot Firebase Admin SDK |
| **AI** | OpenAI GPT API (gpt-5-mini) |
| **온디바이스 ML** | Google ML Kit (Entity Extraction) |
| **애니메이션** | Lottie Android |

---

# 🖥️ 백엔드 (Spring Boot)

## 패키지 구조

```jsx
com.aimong
├── AimongApplication.kt
│
├── global/                          # 전역 공통 모듈
│   ├── config/
│   │   ├── SecurityConfig.kt        # Spring Security 설정
│   │   ├── FirebaseConfig.kt        # Firebase Admin SDK 초기화
│   │   └── OpenAiConfig.kt          # OpenAI 클라이언트 설정
│   ├── filter/
│   │   └── JwtAuthFilter.kt         # OncePerRequestFilter — 토큰 검증 + session_version 체크
│   ├── exception/
│   │   ├── GlobalExceptionHandler.kt  # @RestControllerAdvice
│   │   ├── AimongException.kt
│   │   └── ErrorCode.kt             # 에러 코드 ENUM (BAD_REQUEST, UNAUTHORIZED ...)
│   ├── response/
│   │   └── ApiResponse.kt           # { success, data } / { success, error } 공통 래퍼
│   ├── scheduler/
│   │   ├── DailyResetScheduler.kt   # 매일 00:00 KST — today_xp 리셋, today_mission_count 리셋
│   │   ├── PetMoodScheduler.kt      # 매일 00:01 KST — HAPPY/IDLE/SAD_LIGHT/SAD_DEEP 규칙에 따라 mood 재계산
│   │   ├── WeeklyResetScheduler.kt  # 매주 월 00:00 KST — weekly_xp 리셋
│   │   ├── DailyMissionScheduler.kt # 매일 23:30 KST — GPT로 다음날 AI 미션 생성
│   │   └── FcmReminderScheduler.kt  # 매일 09:00 KST — 미학습 3일 이상 부모 FCM
│   └── util/
│       ├── KstDateUtils.kt          # KST LocalDate/시각 유틸
│       └── SecureRandomUtils.kt     # 6자리 코드 생성, 가챠 SecureRandom
│
├── domain/
│   │
│   ├── auth/                        # 인증 도메인
│   │   ├── controller/
│   │   │   ├── ParentAuthController.kt   # POST /parent/register
│   │   │   └── ChildAuthController.kt    # POST /child/login, PUT regenerate-code
│   │   ├── service/
│   │   │   ├── ParentAuthService.kt
│   │   │   └── ChildAuthService.kt
│   │   ├── repository/
│   │   │   ├── ParentAccountRepository.kt
│   │   │   └── ChildProfileRepository.kt
│   │   ├── entity/
│   │   │   ├── ParentAccount.kt
│   │   │   └── ChildProfile.kt
│   │   └── dto/
│   │       ├── ParentRegisterRequest.kt
│   │       ├── ChildLoginRequest.kt
│   │       └── ChildLoginResponse.kt
│   │
│   ├── mission/                     # 퀘스트/문제 도메인
│   │   ├── controller/
│   │   │   └── MissionController.kt      # GET /missions, GET /questions, POST /submit
│   │   ├── service/
│   │   │   ├── MissionService.kt         # 미션 목록 + 잠금 조건
│   │   │   ├── QuizService.kt            # 문제 조회 + quiz_attempts 생성
│   │   │   ├── SubmitService.kt          # 섹션 11 연쇄 이벤트 전체 오케스트레이션
│   │   │   └── GptQuestionService.kt     # GPT 문제 동적 생성
│   │   ├── repository/
│   │   │   ├── MissionRepository.kt
│   │   │   ├── QuestionBankRepository.kt
│   │   │   ├── QuizAttemptRepository.kt
│   │   │   ├── MissionAttemptRepository.kt
│   │   │   └── MissionDailyProgressRepository.kt
│   │   ├── entity/
│   │   │   ├── Mission.kt
│   │   │   ├── QuestionBank.kt
│   │   │   ├── QuizAttempt.kt
│   │   │   ├── MissionAttempt.kt
│   │   │   └── MissionDailyProgress.kt
│   │   └── dto/
│   │       ├── QuestionResponse.kt
│   │       ├── SubmitRequest.kt
│   │       └── SubmitResponse.kt
│   │
│   ├── pet/                         # 펫 도메인
│   │   ├── controller/
│   │   │   └── PetController.kt          # GET /pet
│   │   ├── service/
│   │   │   ├── PetService.kt             # 장착 펫 조회
│   │   │   └── PetGrowthService.kt       # XP 적립, 성장 단계 체크, 아이몽 달성 처리
│   │   ├── repository/
│   │   │   ├── PetRepository.kt
│   │   │   └── EquippedPetRepository.kt
│   │   ├── entity/
│   │   │   ├── Pet.kt
│   │   │   └── EquippedPet.kt
│   │   └── dto/
│   │       └── PetResponse.kt
│   │
│   ├── gacha/                       # 가챠 도메인
│   │   ├── controller/
│   │   │   └── GachaController.kt        # POST /pull, GET /fragments, POST /exchange
│   │   ├── service/
│   │   │   ├── GachaPullService.kt       # 확률 계산 + 트랜잭션
│   │   │   ├── GachaProbabilityService.kt # srBonus, 구간 확률표 계산
│   │   │   └── FragmentService.kt        # 조각 조회 + 교환
│   │   ├── repository/
│   │   │   ├── TicketRepository.kt
│   │   │   ├── GachaPullRepository.kt
│   │   │   └── FragmentRepository.kt
│   │   ├── entity/
│   │   │   ├── Ticket.kt
│   │   │   ├── GachaPull.kt
│   │   │   └── Fragment.kt
│   │   └── dto/
│   │       ├── GachaPullRequest.kt
│   │       └── GachaPullResponse.kt
│   │
│   ├── streak/                      # 스트릭 도메인
│   │   ├── controller/
│   │   │   └── StreakController.kt        # GET /streak, POST/DELETE /partner
│   │   ├── service/
│   │   │   ├── StreakService.kt           # 스트릭 계산 로직 (섹션 10-1)
│   │   │   ├── MilestoneService.kt        # 고정/사용자 목표 마일스톤 체크
│   │   │   └── FriendStreakService.kt     # 파트너 연결/해제 (대칭 2행 트랜잭션)
│   │   ├── repository/
│   │   │   ├── StreakRecordRepository.kt
│   │   │   ├── FriendStreakRepository.kt
│   │   │   ├── MilestoneRewardRepository.kt
│   │   │   └── StreakMilestoneRepository.kt
│   │   ├── entity/
│   │   │   ├── StreakRecord.kt
│   │   │   ├── FriendStreak.kt
│   │   │   ├── MilestoneReward.kt
│   │   │   └── StreakMilestone.kt
│   │   └── dto/
│   │       └── StreakResponse.kt
│   │
│   ├── quest/                       # 퀘스트/업적 도메인
│   │   ├── controller/
│   │   │   └── QuestController.kt         # GET /daily, GET /weekly, POST /claim, GET /achievements
│   │   ├── service/
│   │   │   ├── DailyQuestService.kt       # 데일리 퀘스트 진행도 체크 + AUTO 자동 지급
│   │   │   ├── WeeklyQuestService.kt
│   │   │   └── AchievementService.kt      # totalXp 기반 프로필 이미지 업적 체크
│   │   ├── repository/
│   │   │   ├── DailyQuestRepository.kt
│   │   │   ├── WeeklyQuestRepository.kt
│   │   │   └── AchievementRepository.kt
│   │   ├── entity/
│   │   │   ├── DailyQuest.kt
│   │   │   ├── WeeklyQuest.kt
│   │   │   └── Achievement.kt
│   │   └── dto/
│   │       ├── QuestResponse.kt
│   │       └── ClaimRequest.kt
│   │
│   ├── chat/                        # 챗봇 도메인
│   │   ├── controller/
│   │   │   └── ChatController.kt          # POST /chat/send
│   │   ├── service/
│   │   │   └── ChatService.kt             # GPT 호출 + 일일 횟수 체크 + 2차 마스킹
│   │   ├── repository/
│   │   │   └── ChatUsageRepository.kt
│   │   ├── entity/
│   │   │   └── ChatUsage.kt
│   │   └── dto/
│   │       ├── ChatRequest.kt
│   │       └── ChatResponse.kt
│   │
│   ├── privacy/                     # 개인정보 도메인
│   │   ├── controller/
│   │   │   └── PrivacyController.kt       # POST /privacy/event
│   │   ├── service/
│   │   │   └── PrivacyEventService.kt     # 이벤트 저장 + FCM 비동기 발송
│   │   ├── repository/
│   │   │   └── PrivacyEventRepository.kt
│   │   ├── entity/
│   │   │   └── PrivacyEvent.kt
│   │   └── dto/
│   │       └── PrivacyEventRequest.kt
│   │
│   ├── reward/                      # 복귀 보상 도메인
│   │   ├── controller/
│   │   │   └── ReturnRewardController.kt  # GET /return-reward, POST /claim
│   │   ├── service/
│   │   │   └── ReturnRewardService.kt
│   │   ├── repository/
│   │   │   └── ReturnRewardClaimRepository.kt
│   │   └── entity/
│   │       └── ReturnRewardClaim.kt
│   │
│   └── parent/                      # 부모 대시보드 도메인
│       ├── controller/
│       │   └── ParentDashboardController.kt  # GET summary, weekly-stats, privacy-log, weak-points
│       └── service/
│           └── ParentDashboardService.kt
│
└── infra/                           # 외부 인프라 어댑터
    ├── fcm/
    │   ├── FcmService.kt            # Firebase Admin SDK FCM 발송
    │   └── FcmPayload.kt
    ├── openai/
    │   └── OpenAiClient.kt          # GPT API HTTP 클라이언트
    └── supabase/
        └── SupabaseProperties.kt    # application.yml 바인딩
```

## 핵심 규칙

```
1. Service 간 직접 호출 금지 — 같은 도메인 내에서만 호출
   도메인 간 데이터가 필요하면 Repository 직접 주입 또는 이벤트 방식 사용
2. SubmitService가 연쇄 이벤트(섹션 11) 전체를 하나의 @Transactional로 오케스트레이션
   내부에서 PetGrowthService, StreakService, DailyQuestService 등을 순서대로 호출
3. FCM 발송은 항상 @Async 비동기 처리 (트랜잭션 외부)
4. 모든 날짜 계산: ZoneId.of("Asia/Seoul") 사용, JVM 타임존도 Asia/Seoul 설정 필수
5. XP 계산: 모든 결과에 Math.floor 적용 (Math.round 사용 금지)
```

---

# 📱 프론트엔드 (Android / Kotlin)

## 아키텍처: MVVM + Clean Architecture

```
[Presentation Layer]  Fragment / Activity / ViewModel / UiState
        ↓  호출
[Domain Layer]        UseCase (비즈니스 로직)
        ↓  호출
[Data Layer]          Repository (Interface) → RepositoryImpl → RemoteDataSource / LocalDataSource
```

## 패키지 구조

```
com.aimong.android
├── AimongApp.kt                     # Application 클래스 (Hilt 초기화)
│
├── core/                            # 공통 기반
│   ├── network/
│   │   ├── AimongApiService.kt      # Retrofit 인터페이스 (전체 API 엔드포인트)
│   │   ├── NetworkModule.kt         # Hilt — Retrofit, OkHttp 제공
│   │   ├── AuthInterceptor.kt       # Bearer 토큰 자동 삽입
│   │   └── ApiResponse.kt           # sealed class Success / Error / Loading
│   ├── local/
│   │   ├── AimongDatabase.kt        # Room DB
│   │   ├── SessionManager.kt        # DataStore — childSessionToken, childId 저장
│   │   └── OfflineQueueDao.kt       # 오프라인 미션 제출 큐
│   ├── ui/
│   │   ├── BaseFragment.kt
│   │   ├── BaseViewModel.kt
│   │   └── UiState.kt               # sealed class Idle / Loading / Success / Error
│   ├── privacy/
│   │   ├── PrivacyRadar.kt          # ML Kit + Regex 1·2차 개인정보 감지 (온디바이스)
│   │   └── MaskingUtils.kt          # 감지 항목 마스킹 처리
│   ├── fcm/
│   │   └── AimongFcmService.kt      # FirebaseMessagingService — 푸시 수신 처리
│   └── util/
│       ├── DateUtils.kt             # KST 날짜 포맷 유틸
│       └── LottieUtils.kt           # Lottie 애니메이션 헬퍼
│
├── feature/
│   │
│   ├── auth/                        # 온보딩 / 로그인
│   │   ├── presentation/
│   │   │   ├── SplashFragment.kt
│   │   │   ├── RoleSelectFragment.kt      # 부모 / 자녀 선택 화면
│   │   │   ├── ParentLoginFragment.kt     # Google Sign-In
│   │   │   ├── ChildCodeFragment.kt       # 6자리 코드 입력
│   │   │   └── AuthViewModel.kt
│   │   ├── domain/
│   │   │   ├── RegisterChildUseCase.kt
│   │   │   └── ChildLoginUseCase.kt
│   │   └── data/
│   │       ├── AuthRepository.kt          # interface
│   │       └── AuthRepositoryImpl.kt
│   │
│   ├── home/                        # 홈 화면 (펫 상태)
│   │   ├── presentation/
│   │   │   ├── HomeFragment.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   └── HomeUiState.kt             # LOADING/HAPPY/IDLE/SAD_LIGHT/SAD_DEEP/PET_EVOLVED/LEVEL_UP
│   │   ├── domain/
│   │   │   └── GetHomeStatusUseCase.kt
│   │   └── data/
│   │       ├── PetRepository.kt
│   │       └── PetRepositoryImpl.kt
│   │
│   ├── mission/                     # 퀘스트 / 퀴즈
│   │   ├── presentation/
│   │   │   ├── MissionListFragment.kt
│   │   │   ├── QuizFragment.kt
│   │   │   ├── QuizResultFragment.kt
│   │   │   ├── MissionViewModel.kt
│   │   │   └── QuizUiState.kt             # LOADING/IDLE/ANSWERED_CORRECT/ANSWERED_WRONG/RESULT/OFFLINE
│   │   ├── domain/
│   │   │   ├── GetMissionListUseCase.kt
│   │   │   ├── GetQuestionsUseCase.kt
│   │   │   └── SubmitAnswersUseCase.kt
│   │   └── data/
│   │       ├── MissionRepository.kt
│   │       └── MissionRepositoryImpl.kt
│   │
│   ├── chat/                        # GPT 챗봇
│   │   ├── presentation/
│   │   │   ├── ChatFragment.kt
│   │   │   ├── ChatViewModel.kt
│   │   │   └── ChatUiState.kt             # IDLE/DETECTING/PRIVACY_WARNING/WAITING_GPT/LIMIT_REACHED
│   │   ├── domain/
│   │   │   └── SendChatMessageUseCase.kt  # 개인정보 레이더 실행 후 API 호출
│   │   └── data/
│   │       ├── ChatRepository.kt
│   │       └── ChatRepositoryImpl.kt
│   │
│   ├── gacha/                       # 가챠 뽑기
│   │   ├── presentation/
│   │   │   ├── GachaFragment.kt
│   │   │   ├── GachaResultFragment.kt
│   │   │   ├── GachaViewModel.kt
│   │   │   └── GachaUiState.kt            # IDLE/NO_TICKET/PULLING/RESULT_NORMAL/RESULT_RARE/...
│   │   ├── domain/
│   │   │   ├── PullGachaUseCase.kt
│   │   │   └── ExchangeFragmentUseCase.kt
│   │   └── data/
│   │       ├── GachaRepository.kt
│   │       └── GachaRepositoryImpl.kt
│   │
│   ├── streak/                      # 스트릭 / 공동 스트릭
│   │   ├── presentation/
│   │   │   ├── StreakFragment.kt
│   │   │   ├── PartnerConnectFragment.kt
│   │   │   └── StreakViewModel.kt
│   │   ├── domain/
│   │   │   ├── GetStreakUseCase.kt
│   │   │   └── ConnectPartnerUseCase.kt
│   │   └── data/
│   │       ├── StreakRepository.kt
│   │       └── StreakRepositoryImpl.kt
│   │
│   ├── quest/                       # 데일리/위클리 퀘스트 + 업적
│   │   ├── presentation/
│   │   │   ├── QuestFragment.kt
│   │   │   ├── AchievementFragment.kt
│   │   │   └── QuestViewModel.kt
│   │   ├── domain/
│   │   │   ├── GetDailyQuestUseCase.kt
│   │   │   ├── GetWeeklyQuestUseCase.kt
│   │   │   └── ClaimQuestRewardUseCase.kt
│   │   └── data/
│   │       ├── QuestRepository.kt
│   │       └── QuestRepositoryImpl.kt
│   │
│   └── parent/                      # 부모 대시보드
│       ├── presentation/
│       │   ├── ParentDashboardFragment.kt
│       │   ├── ParentDashboardViewModel.kt
│       │   └── PrivacyLogFragment.kt
│       ├── domain/
│       │   ├── GetChildSummaryUseCase.kt
│       │   └── GetWeeklyStatsUseCase.kt
│       └── data/
│           ├── ParentRepository.kt
│           └── ParentRepositoryImpl.kt
│
└── navigation/
    ├── nav_child.xml                # 자녀 화면 NavGraph
    └── nav_parent.xml               # 부모 화면 NavGraph
```

## 핵심 규칙

```
1. ViewModel → UseCase만 호출. Repository 직접 호출 금지.
2. UseCase → Repository Interface만 의존. RepositoryImpl은 Data Layer에서만 알고 있음.
3. UiState는 sealed class로 관리. Fragment는 UiState 구독만 담당.
4. 개인정보 레이더(PrivacyRadar)는 ChatViewModel이 아닌 SendChatMessageUseCase 내부에서 실행.
   → 비즈니스 로직이 Domain Layer에 집중됨.
5. 오프라인 큐: SubmitAnswersUseCase에서 네트워크 실패 감지 시 Room OfflineQueueDao에 저장.
   WorkManager로 온라인 복구 시 자동 재전송.
6. Hilt로 의존성 주입 전체 관리.
```

---

# 🔗 도메인 간 의존 관계

```jsx
[auth]
  └─▶ [mission]  ← 퀴즈 제출 시
        └─▶ [pet]       XP 적립 + 성장 체크
        └─▶ [streak]    스트릭 업데이트
        └─▶ [quest]     데일리/위클리/업적 체크

[chat]
  └─▶ [privacy]  개인정보 이벤트 기록 (서버 2차 감지 시)
  └─▶ [quest]    CHAT_GPT 데일리 퀘스트 AUTO 처리

[gacha]
  └─▶ [pet]      신규 펫 INSERT / 중복 시 조각 처리
  └─▶ [infra/fcm] 레벨업 FCM

[privacy]
  └─▶ [infra/fcm] 부모 즉시 FCM

[parent]
  └─▶ [mission]  통계 집계 (is_review=false 최초 완료만)
  └─▶ [streak]   연속 학습일 조회
  └─▶ [privacy]  이력 조회
```

---

# 🌿 브랜치 전략

```
main          ← 배포/발표용. 직접 push 금지.
  └─ develop  ← 통합 브랜치. PR 머지 대상.
        ├─ feature/hanil-auth
        ├─ feature/hanil-mission
        ├─ feature/hanil-gacha
        ├─ feature/dabin-home
        ├─ feature/dabin-quiz
        └─ feature/dabin-chat
```

브랜치 명명 규칙: `feature/{이름}-{기능}` (예: `feature/hanil-streak`, `feature/dabin-gacha-ui`)

---

# 📁 루트 디렉토리 구성

```
AImong/
├── backend/          # Spring Boot 프로젝트
│   ├── src/
│   ├── build.gradle.kts
│   └── application.yml
├── android/          # Android 프로젝트
│   ├── app/src/
│   └── build.gradle.kts
└── prompts/          # AI 코딩 도구 컨텍스트 파일
    ├── context-be.md   # BE 구현 시 Cursor에 첨부할 컨텍스트
    └── context-fe.md   # FE 구현 시 Cursor에 첨부할 컨텍스트
```

---

> 📝 **v1.4**: 최신 문서 기준 정합성 반영. 기능 명세서 v2.3 + API v1.3 + ERD v1.3 기준으로 통일하고, 도메인 의존 관계와 스케줄러 설명을 최신 용어로 정리.
> 

> 🔗 **연관 문서**: 기능 명세서 v2.3 | API 명세서 v1.3 | ERD 설계서 v1.3
>