package com.kduniv.aimong.feature.mission.domain.model

data class QuizSession(
    val attemptId: String,
    val questions: List<Question>
)

data class Question(
    val id: String,
    val type: QuestionType,
    val typeLabel: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int, // 주의: 서버에서 안 내려줄 경우 대비 필요
    val feedback: String
)

enum class QuestionType {
    OX, MULTIPLE, FILL, SITUATION
}

data class QuizResult(
    val score: Int,
    val totalQuestions: Int,
    val isSuccess: Boolean,
    val gainedExp: Int,
    val petBonusExp: Int,
    val results: List<QuestionResult> = emptyList()
)

data class QuestionResult(
    val questionId: String,
    val isCorrect: Boolean,
    val explanation: String
)
