package com.kduniv.aimong.feature.quiz.domain.model

data class QuizQuestions(
    val missionId: String,
    val missionTitle: String,
    val isReview: Boolean,
    val quizAttemptId: String,
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
    val score: Int,
    val total: Int,
    val isPassed: Boolean,
    val isPerfect: Boolean,
    val xpEarned: Int,
    val bonusXp: Int = 0,
    val bonusReason: String? = null,
    val petEvolved: Boolean,
    val streakDays: Int,
    val rewards: List<QuizReward> = emptyList(),
    val results: List<QuestionResult>
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
