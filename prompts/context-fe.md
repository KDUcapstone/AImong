# AImong 프론트엔드 AI 코딩 컨텍스트 (Cursor/Copilot용)

## 프로젝트 정보
- **플랫폼**: Android (Kotlin, minSdk 26)
- **패키지**: `com.kduniv.aimong`
- **아키텍처**: MVVM + Clean Architecture (Presentation → Domain → Data)
- **DI**: Hilt
- **네트워크**: Retrofit + OkHttp
- **애니메이션**: Lottie
- **ML**: Google ML Kit (Entity Extraction)
- **로컬 저장**: Room DB + DataStore

## 아키텍처 원칙
1. **ViewModel → UseCase만 호출** (Repository 직접 호출 금지)
2. **UseCase → Repository Interface만 의존** (RepositoryImpl은 Data Layer만)
3. **UiState는 sealed class/enum으로 관리** (Fragment는 구독만)
4. **개인정보 레이더(PrivacyRadar)**는 ChatViewModel이 아닌 SendChatMessageUseCase 내부에서 실행
5. **오프라인 큐**: SubmitAnswersUseCase에서 네트워크 실패 감지 시 Room에 저장, WorkManager 재전송

## 패키지 구조
```
com.kduniv.aimong/
├── core/         # 공통 기반 (network, local, ui, privacy, fcm, util)
├── feature/      # 기능별 (auth, home, mission, chat, gacha, streak, quest, parent)
│   └── {feature}/
│       ├── presentation/  # Fragment, ViewModel, UiState
│       ├── domain/        # UseCase
│       └── data/          # Repository interface + impl
└── navigation/   # NavGraph XML
```

## 개인정보 레이더 (PrivacyRadar)
- 1차: ML Kit Entity Extraction (전화번호, 이메일, 주소, URL)
- 2차: Regex (이름, 학교명, 나이, 학년, 주소 보조)
- 감지 → 경고 카드 → 사용자 선택 → 마스킹 후 전송

## 펫 XP 규칙
- 알: 0 ≤ pet.xp < 80
- 성장: 80 ≤ pet.xp < 250
- 아이몽: pet.xp ≥ 250 → xp 리셋, 왕관 영구 장착

## 가챠 UiState
IDLE → PULLING → RESULT_NORMAL/RESULT_RARE/RESULT_SR/RESULT_LEGEND
