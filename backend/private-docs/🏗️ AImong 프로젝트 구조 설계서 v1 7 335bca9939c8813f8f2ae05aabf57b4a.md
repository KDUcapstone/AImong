# 🏗️ AImong 프로젝트 구조 설계서 v1.7

생성자: 한욱 
카테고리: 기술 문서
최종 업데이트 시간: May 27, 2026 3:28 PM
기준일: May 27, 2026
문서 종류: 시스템 설계
버전: v1.7
허브 분류: 구현 문서

> **버전**: v1.7 \| **수정일**: 2026-05-27 \| **기반**: 기능 명세서 v4.4 + API 명세서 v3.8 + ERD 설계서 v2.10 + UI/UX 화면 설계서 v2.1 + 문제 생성 시스템 설계서 v1.8
> 

> 
> 

> **v1.6 → v1.7 변경 사유**: 퀘스트 수동 수령 정책과 불꽃 방패 수동 사용 API를 구조 문서에 반영합니다.
> 

> **v1.5 → v1.6 변경 사유**: Java 21 기준으로 백엔드 구조를 정정하고, 최신 기능/API/ERD에 맞춰 에너지, 재화 원장, 챗봇 세션/메시지, 하트 부활, 스트릭 보호권 구매 구조를 반영합니다.
> 

> 🤖 **이 문서는 AI 코딩 도구(Cursor, Copilot 등)가 참고하기 쉽도록 작성되었습니다.**
> 

---

# 📐 설계 원칙

| 항목 | 결정 사항 |
| --- | --- |
| **BE 패키지 구조** | 혼합형 — 도메인별 패키지 안에 controller/service/repository 계층 구분 |
| **FE 아키텍처** | MVVM + Clean Architecture (Presentation → Domain → Data 3레이어) |
| **BE 프레임워크** | Spring Boot 3.x (Java 21) |
| **FE 언어** | Kotlin (Android minSdk 26) |
| **DB** | Supabase (PostgreSQL) — Spring Data JPA + Supabase REST |
| **인증** | Firebase Auth (Google 로그인) + 서버 발급 CHILD JWT/session_version 검증. Supabase Auth는 MVP 인증 주체로 사용하지 않음 |
| **푸시** | FCM — Spring Boot Firebase Admin SDK |
| **AI** | OpenAI GPT API (gpt-5-mini) |
| **온디바이스 ML** | Google ML Kit (Entity Extraction) |
| **애니메이션** | Lottie Android |

---

# 🖥️ 백엔드 (Spring Boot)

## 패키지 구조

```
com.aimong
├── AimongApplication.java
│
├── global/                              # 전역 공통 모듈
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security 설정
│   │   ├── FirebaseConfig.java          # Firebase Admin SDK 초기화
│   │   ├── OpenAiConfig.java            # OpenAI 클라이언트 설정
│   │   ├── AsyncConfig.java             # FCM 등 비동기 처리 설정
│   │   └── JpaConfig.java               # JPA Auditing, 시간대 등 공통 설정
│   ├── filter/
│   │   └── JwtAuthFilter.java           # Firebase ID Token / CHILD JWT / session_version 검증
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   ├── AimongException.java
│   │   └── ErrorCode.java               # 내부 enum명과 외부 error.code 매핑 관리
│   ├── response/
│   │   └── ApiResponse.java             # { success, data } / { success, error } 공통 응답 래퍼
│   ├── scheduler/
│   │   ├── DailyResetScheduler.java      # 매일 00:00 KST — today_xp 등 일별 값 리셋
│   │   ├── PetMoodScheduler.java         # 매일 00:01 KST — 펫 감정 상태 재계산
│   │   ├── WeeklyResetScheduler.java     # 매주 월 00:00 KST — weekly_xp 리셋
│   │   └── FcmReminderScheduler.java     # 학습 리마인드 / 복귀 알림 FCM
│   └── util/
│       ├── KstDateUtils.java            # KST LocalDate/시각 유틸
│       └── SecureRandomUtils.java       # 6자리 코드 생성, 가챠 SecureRandom
│
├── domain/
│   │
│   ├── auth/                            # 인증 / 부모-자녀 계정 도메인
│   │   ├── controller/
│   │   │   ├── ParentAuthController.java # POST /parent/register, GET /parent/me, POST /parent/logout, DELETE /parent/account, DELETE /parent/fcm-token
│   │   │   ├── ChildAuthController.java  # POST /child/login, GET /child/me, POST /child/logout, DELETE /child/fcm-token
│   │   │   └── ParentChildController.java# POST/GET/PATCH/DELETE /parent/children, PUT /parent/child/{childId}/regenerate-code
│   │   ├── service/
│   │   │   ├── ParentAuthService.java
│   │   │   ├── ChildAuthService.java
│   │   │   └── ParentChildService.java   # 자녀 최대 3명 제한, soft delete, session_version 무효화
│   │   ├── repository/
│   │   │   ├── ParentAccountRepository.java
│   │   │   └── ChildProfileRepository.java
│   │   ├── entity/
│   │   │   ├── ParentAccount.java
│   │   │   └── ChildProfile.java         # equipped_pet_id, gear, energy, shield_count, session_version 포함
│   │   └── dto/
│   │       ├── ParentRegisterRequest.java
│   │       ├── ChildLoginRequest.java
│   │       └── ChildLoginResponse.java
│   │
│   ├── bootstrap/                       # 앱 부팅 / 자동 라우팅
│   │   ├── controller/
│   │   │   └── BootstrapController.java  # GET /app/bootstrap
│   │   ├── service/
│   │   │   └── BootstrapService.java     # 인증 주체 판단 + 초기 화면 + appConfig 조립
│   │   └── dto/
│   │       └── BootstrapResponse.java
│   │
│   ├── mission/                         # 미션 / 퀴즈 / 에너지 / attempt 도메인
│   │   ├── controller/
│   │   │   ├── MissionController.java        # GET /missions, GET /missions/{missionId}/status, GET /mission-sets/{setId}/questions
│   │   │   ├── MissionSetController.java     # POST /mission-sets/{setId}/check, POST /mission-sets/{setId}/submit, GET /mission-sets/{setId}/report
│   │   │   ├── MissionAttemptController.java # GET /mission-attempts/{attemptId}, POST /mission-attempts/{attemptId}/abandon, POST /mission-attempts/{attemptId}/revive
│   │   │   └── EnergyController.java         # GET /energy, POST /energy/add
│   │   ├── service/
│   │   │   ├── MissionService.java           # 학습맵, 잠금/복습/완료 상태 조회
│   │   │   ├── MissionStatusService.java     # 개별 미션 진입 전 상태 확인
│   │   │   ├── MissionQuestionSetFactory.java# setId → missionId/starLevel 확인 후 런타임 10문항 구성
│   │   │   ├── QuizService.java              # 문제 조회, quiz_attempts 생성, termHints 조립, energy 차감
│   │   │   ├── MissionCheckService.java      # 단일 문항 check, 정답/해설 즉시 피드백, 보상 미반영
│   │   │   ├── MissionSubmitService.java     # 최종 submit, 보상/진행도/통계 저장
│   │   │   ├── MissionAttemptService.java    # attempt 복구/포기/하트 부활/상태 검증
│   │   │   ├── EnergyService.java            # 에너지 lazy recovery, 수동 +5 추가
│   │   │   └── TermHintService.java          # 고정 사전 기반 어려운 개념어 보강설명
│   │   ├── repository/
│   │   │   ├── MissionRepository.java
│   │   │   ├── MissionSetRepository.java
│   │   │   ├── QuestionBankRepository.java
│   │   │   ├── QuestionAnswerKeyRepository.java
│   │   │   ├── QuizAttemptRepository.java
│   │   │   ├── MissionAttemptRepository.java
│   │   │   ├── MissionAnswerResultRepository.java
│   │   │   ├── MissionSetProgressRepository.java
│   │   │   └── MissionDailyProgressRepository.java
│   │   ├── entity/
│   │   │   ├── Mission.java
│   │   │   ├── MissionSet.java
│   │   │   ├── QuestionBank.java
│   │   │   ├── QuestionAnswerKey.java
│   │   │   ├── QuizAttempt.java            # question_ids_json, answered_question_ids_json, remaining_lives, revive_count, status
│   │   │   ├── MissionAttempt.java
│   │   │   ├── MissionAnswerResult.java
│   │   │   ├── MissionSetProgress.java
│   │   │   └── MissionDailyProgress.java
│   │   └── dto/
│   │       ├── MissionMapResponse.java
│   │       ├── QuestionResponse.java       # termHints 포함
│   │       ├── TermHintResponse.java
│   │       ├── CheckRequest.java
│   │       ├── CheckResponse.java
│   │       ├── SubmitRequest.java
│   │       ├── SubmitResponse.java         # score, correctCount, rewards { gear, exp, fragments }
│   │       ├── AttemptResponse.java
│   │       └── EnergyResponse.java
│   │
│   ├── wallet/                          # 재화 / 원장 도메인
│   │   ├── controller/
│   │   │   └── WalletController.java      # GET /wallet
│   │   ├── service/
│   │   │   └── CurrencyTransactionService.java # gear 증감, MISSION_CLEAR/HEART_REVIVE/STREAK_SHIELD_PURCHASE 원장 기록
│   │   ├── repository/
│   │   │   └── CurrencyTransactionRepository.java
│   │   ├── entity/
│   │   │   └── CurrencyTransaction.java
│   │   └── dto/
│   │       └── WalletResponse.java
│   │
│   ├── pet/                             # 펫 도메인
│   │   ├── controller/
│   │   │   └── PetController.java         # GET /pet
│   │   ├── service/
│   │   │   ├── PetService.java            # child_profiles.equipped_pet_id 기준 장착 펫 조회
│   │   │   └── PetGrowthService.java      # XP 적립, 성장 단계 체크, 아이몽 달성 처리
│   │   ├── repository/
│   │   │   └── PetRepository.java
│   │   ├── entity/
│   │   │   └── Pet.java
│   │   └── dto/
│   │       └── PetResponse.java
│   │
│   ├── gacha/                           # 가챠 / 조각 도메인
│   │   ├── controller/
│   │   │   └── GachaController.java       # POST /pull, GET /fragments, POST /exchange
│   │   ├── service/
│   │   │   ├── GachaPullService.java
│   │   │   ├── GachaProbabilityService.java
│   │   │   └── FragmentService.java
│   │   ├── repository/
│   │   │   ├── TicketRepository.java
│   │   │   ├── GachaPullRepository.java
│   │   │   └── PetFragmentRepository.java
│   │   ├── entity/
│   │   │   ├── Ticket.java
│   │   │   ├── GachaPull.java
│   │   │   └── PetFragment.java           # id UUID PK, UNIQUE(child_id, grade)
│   │   └── dto/
│   │       ├── GachaPullRequest.java
│   │       └── GachaPullResponse.java
│   │
│   ├── streak/                          # 스트릭 / 보호권 도메인
│   │   ├── controller/
│   │   │   └── StreakController.java      # GET /streak, POST /streak/shields/purchase, POST /streak/shields/use
│   │   ├── service/
│   │   │   ├── StreakService.java         # 연속 학습일 계산, RECOVERABLE/PROTECTED 상태 전이
│   │   │   ├── StreakShieldService.java   # child_profiles.shield_count 기준 구매/자동 사용/수동 사용
│   │   │   └── MilestoneService.java      # 마일스톤 보상 체크
│   │   ├── repository/
│   │   │   ├── StreakRecordRepository.java
│   │   │   └── MilestoneRewardRepository.java
│   │   ├── entity/
│   │   │   ├── StreakRecord.java
│   │   │   └── MilestoneReward.java
│   │   └── dto/
│   │       ├── StreakResponse.java
│   │       ├── ShieldPurchaseResponse.java
│   │       └── ShieldUseResponse.java
│   │
│   ├── quest/                           # 데일리/위클리 퀘스트 + 업적
│   │   ├── controller/
│   │   │   └── QuestController.java       # GET /daily, GET /weekly, POST /claim, GET /achievements
│   │   ├── service/
│   │   │   ├── DailyQuestService.java
│   │   │   ├── WeeklyQuestService.java
│   │   │   └── AchievementService.java
│   │   ├── repository/
│   │   │   ├── DailyQuestProgressRepository.java
│   │   │   ├── WeeklyQuestProgressRepository.java
│   │   │   └── AchievementProgressRepository.java
│   │   ├── entity/
│   │   │   ├── DailyQuestProgress.java
│   │   │   ├── WeeklyQuestProgress.java
│   │   │   └── AchievementProgress.java
│   │   └── dto/
│   │       ├── QuestResponse.java
│   │       └── ClaimRequest.java
│   │
│   ├── chat/                            # GPT 챗봇 도메인
│   │   ├── controller/
│   │   │   └── ChatController.java        # POST /chat/send
│   │   ├── service/
│   │   │   ├── ChatService.java           # GPT 호출, 일일 횟수 제한, 세션 응답 조립
│   │   │   ├── ChatSessionService.java    # chat_sessions 생성/조회/만료 처리
│   │   │   └── ChatMaskingService.java    # 원문 미저장, 마스킹 메시지 저장
│   │   ├── repository/
│   │   │   ├── ChatUsageRepository.java
│   │   │   ├── ChatSessionRepository.java
│   │   │   └── ChatMessageRepository.java
│   │   ├── entity/
│   │   │   ├── ChatUsage.java             # PRIMARY KEY(child_id, usage_date)
│   │   │   ├── ChatSession.java
│   │   │   └── ChatMessage.java           # role, content_masked 저장
│   │   └── dto/
│   │       ├── ChatRequest.java           # message, masked, sessionId(optional)
│   │       └── ChatResponse.java          # reply, remainingCalls, sessionId, sessionExpiresAt
│   │
│   ├── notification/                    # 알림 설정 도메인
│   │   ├── controller/
│   │   │   └── NotificationSettingsController.java # GET/PATCH /notification/settings
│   │   ├── service/
│   │   │   └── NotificationSettingsService.java
│   │   ├── repository/
│   │   │   └── ParentNotificationSettingsRepository.java
│   │   ├── entity/
│   │   │   └── ParentNotificationSettings.java
│   │   └── dto/
│   │       └── NotificationSettingsResponse.java
│   │
│   ├── privacy/                         # 개인정보 감지 이벤트 도메인
│   │   ├── controller/
│   │   │   └── PrivacyController.java     # POST /privacy/event
│   │   ├── service/
│   │   │   └── PrivacyEventService.java   # 이벤트 저장 + 부모 FCM 비동기 발송
│   │   ├── repository/
│   │   │   └── PrivacyEventRepository.java
│   │   ├── entity/
│   │   │   └── PrivacyEvent.java
│   │   └── dto/
│   │       └── PrivacyEventRequest.java
│   │
│   ├── reward/                          # 복귀 보상 도메인
│   │   ├── controller/
│   │   │   └── ReturnRewardController.java # GET /return-reward, POST /claim
│   │   ├── service/
│   │   │   └── ReturnRewardService.java
│   │   ├── repository/
│   │   │   └── ReturnRewardClaimRepository.java
│   │   └── entity/
│   │       └── ReturnRewardClaim.java
│   │
│   └── parent/                          # 부모 대시보드 도메인
│       ├── controller/
│       │   └── ParentDashboardController.java # GET summary, weekly-stats, privacy-log, weak-points
│       └── service/
│           └── ParentDashboardService.java
│
└── infra/                               # 외부 인프라 어댑터
    ├── fcm/
    │   ├── FcmService.java
    │   └── FcmPayload.java
    ├── openai/
    │   └── OpenAiClient.java
    └── supabase/
        └── SupabaseProperties.java
```

> ⚠️ 공동 스트릭 파트너 연결/해제(`friend_streaks`, `POST/DELETE /partner`)는 현재 스트릭 API 기준 Post-MVP 후보로 분리합니다. MVP 구조에는 보호권 구매/사용과 개인 스트릭 조회만 포함합니다.
> 

## 핵심 규칙

```jsx
1. Service 간 직접 호출은 오케스트레이션 서비스에서만 허용한다. SubmitService, BootstrapService처럼 여러 도메인 요약이 필요한 경우 의존 방향을 명시한다.
   일반 도메인 서비스는 같은 도메인 내 호출 또는 Repository/이벤트 방식을 우선한다.
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

```jsx
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
│   │   ├── SessionManager.kt        # DataStore — parent/child token, childId, activeChildId 저장
│   │   └── AttemptCacheDao.kt       # 진행 중 attempt UI 복구용 로컬 캐시 (제출 큐 아님)
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
│   ├── auth/                        # 온보딩 / 로그인 / bootstrap 라우팅
│   │   ├── presentation/
│   │   │   ├── SplashFragment.kt          # GET /app/bootstrap 후 PARENT/CHILD/GUEST 라우팅
│   │   │   ├── RoleSelectFragment.kt      # 부모 / 자녀 선택 화면
│   │   │   ├── ParentLoginFragment.kt     # Google Sign-In
│   │   │   ├── ChildCodeFragment.kt       # 6자리 코드 입력
│   │   │   └── AuthViewModel.kt
│   │   ├── domain/
│   │   │   ├── BootstrapUseCase.kt
│   │   │   ├── ParentLogoutUseCase.kt
│   │   │   ├── ChildLogoutUseCase.kt
│   │   │   ├── DeleteAccountUseCase.kt
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
│   │   │   ├── GetHomeStatusUseCase.kt
│   │   │   └── GetEnergyUseCase.kt
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
│   │   │   ├── GetMissionStatusUseCase.kt
│   │   │   ├── RestoreAttemptUseCase.kt
│   │   │   ├── AbandonAttemptUseCase.kt
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
│   └── parent/                      # 부모 대시보드 / 자녀 관리 / 알림 설정
│       ├── presentation/
│       │   ├── ParentDashboardFragment.kt
│       │   ├── ParentDashboardViewModel.kt
│       │   ├── ChildManageFragment.kt
│       │   ├── NotificationSettingsFragment.kt
│       │   └── PrivacyLogFragment.kt
│       ├── domain/
│       │   ├── GetParentMeUseCase.kt
│       │   ├── GetChildSummaryUseCase.kt
│       │   ├── AddChildUseCase.kt
│       │   ├── UpdateChildUseCase.kt
│       │   ├── DeleteChildUseCase.kt
│       │   ├── GetNotificationSettingsUseCase.kt
│       │   ├── UpdateNotificationSettingsUseCase.kt
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

```jsx
1. ViewModel → UseCase만 호출. Repository 직접 호출 금지.
2. UseCase → Repository Interface만 의존. RepositoryImpl은 Data Layer에서만 알고 있음.
3. UiState는 sealed class로 관리. Fragment는 UiState 구독만 담당.
4. 개인정보 레이더(PrivacyRadar)는 ChatViewModel이 아닌 SendChatMessageUseCase 내부에서 실행.
   → 비즈니스 로직이 Domain Layer에 집중됨.
5. MVP에서는 오프라인 제출 큐를 사용하지 않는다. 네트워크 실패 시 제출을 차단하고, 진행 중 attempt는 서버 복구 API로 이어가기/포기만 제공한다.
6. Hilt로 의존성 주입 전체 관리.
```

---

# 🔗 도메인 간 의존 관계

```jsx
[auth]
  └─▶ [bootstrap] ← 앱 부팅 자동 라우팅
  └─▶ [mission]  ← 퀴즈 제출 시
        └─▶ [pet]       XP 적립 + 성장 체크
        └─▶ [streak]    스트릭 업데이트
        └─▶ [quest]     데일리/위클리/업적 체크

[chat]
  └─▶ [privacy]  개인정보 이벤트 기록 (서버 2차 감지 시)
  └─▶ [quest]    CHAT_GPT 데일리 퀘스트 완료 처리. 보상은 수동 claim에서 지급

[gacha]
  └─▶ [pet]      신규 펫 INSERT / 중복 시 조각 처리
  └─▶ [infra/fcm] 레벨업 FCM

[privacy]
  └─▶ [infra/fcm] 부모 즉시 FCM

[notification]
  └─▶ [infra/fcm] 발송 전 수신 설정 확인

[parent]
  └─▶ [auth]     자녀 추가/수정/삭제, 회원탈퇴 세션 무효화
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

> 📝 **v1.7**: API 명세서 v3.8 반영. 퀘스트 자동 보상 지급 표현을 제거하고, `POST /streak/shields/use`, `ShieldUseResponse`, 불꽃 방패 수동 사용 책임을 구조에 추가.
> 

> 📝 **v1.5**: API 명세서 v2.5 반영. bootstrap, 부모/자녀 me·logout·FCM token 삭제·회원탈퇴, 자녀 관리, 알림 설정, 에너지 조회, 미션 상태 조회, attempt 복구/포기 모듈을 BE/Android 구조에 추가. 오프라인 제출 큐는 MVP 제외로 정리.
> 

> 🔗 **연관 문서**: 기능 명세서 v4.4 \\\| API 명세서 v3.8 \\\| ERD 설계서 v2.10 \\\| UI/UX 화면 설계서 v2.1
>