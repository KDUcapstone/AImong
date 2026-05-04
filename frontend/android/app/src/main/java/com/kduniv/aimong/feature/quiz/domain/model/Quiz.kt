package com.kduniv.aimong.feature.quiz.domain.model

data class QuizQuestions(
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
    val options: List<String>?
)

enum class QuestionType {
    OX, MULTIPLE, FILL, SITUATION
}

data class QuizResult(
    /** 서버 `normal` | `review` */
    val mode: String = "normal",
    val progressApplied: Boolean = false,
    val attemptState: String = "submitted",
    val streakBonusApplied: Boolean = false,
    val score: Int,
    val total: Int,
    val wrongCount: Int,
    val isPassed: Boolean,
    val isPerfect: Boolean,
    val equippedPetGrade: String? = null,
    val xpEarned: Int,
    val bonusXp: Int = 0,
    val bonusReason: String? = null,
    val petEvolved: Boolean,
    val streakDays: Int,
    val todayMissionCount: Int = 0,
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
    val explanation: String
)
