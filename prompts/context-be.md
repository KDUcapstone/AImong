# AImong 백엔드 AI 코딩 컨텍스트 (Cursor/Copilot용)

## 프로젝트 정보
- **프레임워크**: Spring Boot 3.x (Java 17)
- **패키지**: `com.aimong.backend`
- **DB**: Supabase (PostgreSQL) + Spring Data JPA
- **인증**: Firebase Admin SDK (부모) + 자체 세션 토큰 (자녀)
- **AI**: OpenAI GPT API (gpt-4o-mini)
- **FCM**: Firebase Admin SDK

## 패키지 구조 원칙
- 혼합형: 도메인별 패키지 안에 controller/service/repository/entity/dto 계층 분리
- global/: 전역 공통 모듈 (config, filter, exception, response, scheduler, util)
- domain/: auth, mission, pet, gacha, streak, quest, chat, privacy, reward, parent
- infra/: fcm, openai, supabase

## 핵심 규칙 (반드시 준수)
1. **Service 간 직접 호출 금지** — 같은 도메인 내에서만 호출
2. **SubmitService**가 섹션 11 연쇄 이벤트 전체를 하나의 @Transactional로 오케스트레이션
3. **FCM 발송은 항상 @Async 비동기** (트랜잭션 외부)
4. **모든 날짜**: ZoneId.of("Asia/Seoul") 사용
5. **XP 계산**: 반드시 Math.floor 적용 (Math.round 사용 금지)
6. **가챠 난수**: 반드시 서버 사이드 SecureRandom
7. **정답 노출 금지**: 문제 조회 API에 answer/explanation 절대 미포함

## 미션 완료 연쇄 이벤트 (섹션 11)
POST /api/missions/{id}/submit 처리 순서:
① 채점 → ② XP 계산 (공동스트릭 1.5배) → ③ XP 적립 (pet.xp + totalXp + todayXp + weeklyXp)
→ ④ 데일리/위클리/업적 체크 → ⑤ 펫 성장 단계 체크 → ⑥ 스트릭 업데이트
→ ⑦ 마일스톤 체크 → ⑧ 데일리퀘스트 3개 체크 → ⑨ FCM 판단 → ⑩ 응답 반환

## API 응답 형식
- 성공: `{ "success": true, "data": T }`
- 실패: `{ "success": false, "error": { "code": "ERROR_CODE", "message": "..." } }`
