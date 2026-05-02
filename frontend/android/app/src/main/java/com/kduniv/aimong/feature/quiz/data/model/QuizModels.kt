package com.kduniv.aimong.feature.quiz.data.model

import com.google.gson.annotations.SerializedName

data class QuizQuestionsResponse(
    @SerializedName("missionId") val missionId: String,
    @SerializedName("missionTitle") val missionTitle: String,
    @SerializedName("isReview") val isReview: Boolean,
    @SerializedName("quizAttemptId") val quizAttemptId: String,
    @SerializedName("questionCount") val questionCount: Int,
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("questions") val questions: List<QuestionResponse>
)

data class QuestionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // OX | MULTIPLE | FILL | SITUATION
    @SerializedName("question") val question: String,
    @SerializedName("options") val options: List<String>?
)

data class QuizSubmitRequest(
    @SerializedName("quizAttemptId") val quizAttemptId: String,
    @SerializedName("answers") val answers: List<QuizAnswer>
)

data class QuizAnswer(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("selected") val selected: String
)

data class QuizSubmitResponse(
    @SerializedName("score") val score: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("wrongCount") val wrongCount: Int,
    @SerializedName("isPassed") val isPassed: Boolean,
    @SerializedName("isPerfect") val isPerfect: Boolean,
    @SerializedName("equippedPetGrade") val equippedPetGrade: String?,
    @SerializedName("bonusXp") val bonusXp: Int,
    @SerializedName("bonusReason") val bonusReason: String?,
    @SerializedName("xpEarned") val xpEarned: Int,
    @SerializedName("equippedPetXp") val equippedPetXp: Int,
    @SerializedName("petStage") val petStage: String?,
    @SerializedName("petEvolved") val petEvolved: Boolean,
    @SerializedName("streakDays") val streakDays: Int,
    @SerializedName("todayMissionCount") val todayMissionCount: Int,
    @SerializedName("rewards") val rewards: List<RewardResponse>,
    @SerializedName("results") val results: List<QuestionResultResponse>,
    @SerializedName("currentLevel") val currentLevel: Int?,
    @SerializedName("currentXp") val currentXp: Int?,
    @SerializedName("nextLevelXp") val nextLevelXp: Int?
)

data class RewardResponse(
    @SerializedName("type") val type: String,
    @SerializedName("ticketType") val ticketType: String?,
    @SerializedName("count") val count: Int,
    @SerializedName("reason") val reason: String?
)

data class QuestionResultResponse(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("isCorrect") val isCorrect: Boolean,
    @SerializedName("explanation") val explanation: String
)
