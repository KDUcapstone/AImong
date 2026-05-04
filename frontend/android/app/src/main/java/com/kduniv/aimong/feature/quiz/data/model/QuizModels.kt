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
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("progressApplied") val progressApplied: Boolean? = null,
    @SerializedName("attemptState") val attemptState: String? = null,
    @SerializedName("score") val score: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("wrongCount") val wrongCount: Int,
    @SerializedName("isPassed") val isPassed: Boolean,
    @SerializedName("isPerfect") val isPerfect: Boolean,
    @SerializedName("equippedPetGrade") val equippedPetGrade: String? = null,
    @SerializedName("bonusXp") val bonusXp: Int? = null,
    @SerializedName("bonusReason") val bonusReason: String? = null,
    @SerializedName("xpEarned") val xpEarned: Int,
    @SerializedName("streakBonusApplied") val streakBonusApplied: Boolean? = null,
    @SerializedName("equippedPetXp") val equippedPetXp: Int? = null,
    @SerializedName("petStage") val petStage: String? = null,
    @SerializedName("petEvolved") val petEvolved: Boolean? = null,
    @SerializedName("streakDays") val streakDays: Int,
    @SerializedName("todayMissionCount") val todayMissionCount: Int? = null,
    @SerializedName("rewards") val rewards: List<RewardResponse>? = null,
    @SerializedName("remainingTickets") val remainingTickets: RemainingTicketsResponse? = null,
    @SerializedName("results") val results: List<QuestionResultResponse>? = null,
    @SerializedName("currentLevel") val currentLevel: Int? = null,
    @SerializedName("currentXp") val currentXp: Int? = null,
    @SerializedName("nextLevelXp") val nextLevelXp: Int? = null
)

data class RemainingTicketsResponse(
    @SerializedName("normal") val normal: Int = 0,
    @SerializedName("rare") val rare: Int = 0,
    @SerializedName("epic") val epic: Int = 0
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
