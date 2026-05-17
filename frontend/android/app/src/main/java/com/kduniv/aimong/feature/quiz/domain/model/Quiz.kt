package com.kduniv.aimong.feature.quiz.domain.model

data class QuizQuestions(
    val setId: String,
    val missionId: String,
    val missionTitle: String,
    val isReview: Boolean,
    val quizAttemptId: String,
    /** 서버 명세 `questionCount`(보통 10). UI·검증용 */
    val questionCount: Int,
    val expiresAt: String,
    val questions: List<Question>
)

data class Question(
    val id: String,
    val type: QuestionType,
    val question: String,
    val options: List<String>?,
    /** API v1.5 문항 난이도. 없거나 파싱 실패 시 null. */
    val difficulty: QuestionDifficulty? = null,
    /** GET questions의 `answerFormat` — check/submit 시 답 문자열 규칙(미지정 시 보기 문구 전송) */
    val answerFormat: String? = null,
    /** v2.8: 용어 보강설명 (없으면 빈 목록) */
    val termHints: List<TermHint> = emptyList()
)

enum class QuestionType {
    OX, MULTIPLE, FILL, SITUATION
}

data class QuizResult(
    /** 서버 `normal` | `review` */
    val mode: String = "normal",
    val progressApplied: Boolean = false,
    val attemptState: String = AttemptStatus.SUBMITTED.name,
    val streakBonusApplied: Boolean = false,
    val score: Int,
    val total: Int,
    val wrongCount: Int,
    val isPassed: Boolean,
    val isPerfect: Boolean,
    val isFirstClear: Boolean = false,
    val equippedPetGrade: String? = null,
    val xpEarned: Int,
    val bonusXp: Int = 0,
    val bonusReason: String? = null,
    val petEvolved: Boolean,
    val streakDays: Int,
    val todaySetCount: Int = 0,
    val rewards: List<QuizReward> = emptyList(),
    val remainingTickets: RemainingTickets? = null,
    val results: List<QuestionResult>,
    val currentLevel: Int = 1,
    val currentXp: Int = 0,
    val nextLevelXp: Int = 100
)

data class RemainingTickets(
    val normal: Int,
    val rare: Int,
    val epic: Int
)

/** POST …/questions/{questionId}/report 응답 */
data class QuestionReportResult(
    val questionId: String,
    val issueId: String,
    val issueStatus: String,
    val quarantined: Boolean
)

data class QuizReward(
    val type: String,
    val ticketType: String?,
    val count: Int,
    val reason: String?
)

data class QuestionResult(
    val questionId: String,
    val isCorrect: Boolean,
    val explanation: String,
    /** v2.5 리포트 API 등 확장 필드 */
    val questionNo: Int? = null,
    val correctAnswer: String? = null,
    val submittedAnswer: String? = null
)

