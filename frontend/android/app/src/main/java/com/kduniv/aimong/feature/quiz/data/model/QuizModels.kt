package com.kduniv.aimong.feature.quiz.data.model

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.kduniv.aimong.feature.quiz.data.gson.AttemptIdStringAdapter
import com.kduniv.aimong.feature.quiz.data.gson.SubmitRewardsListAdapter

/** GET .../questions 응답 — v2.11: setId는 진행·채점 단위, 문항은 missionId+difficulty 풀에서 런타임 선택 */
data class QuizQuestionsResponse(
    /** v2.4: 서버가 문자열 setId를 줄 수 있어 String으로 수용 */
    @SerializedName("setId") val setId: String? = null,
    /** v2.4: missionId가 UUID(String)일 수 있어 String으로 수용 */
    @SerializedName("missionId") val missionId: String? = null,
    @SerializedName("missionCode") val missionCode: String? = null,
    @SerializedName("starLevel") val starLevel: Int? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("variantNo") val variantNo: Int? = null,
    @SerializedName("missionTitle") val missionTitle: String? = null,
    @SerializedName("isReview") val isReview: Boolean = false,
    /** v2.4: attemptId, v2.3 호환: quizAttemptId */
    @SerializedName(value = "attemptId", alternate = ["quizAttemptId"])
    val attemptId: String? = null,
    @SerializedName("questionCount") val questionCount: Int = 10,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("energyCost") val energyCost: Int? = null,
    @SerializedName("energyBefore") val energyBefore: Int? = null,
    @SerializedName("energyAfter") val energyAfter: Int? = null,
    @SerializedName("questions") val questions: List<QuestionResponse> = emptyList()
)

data class QuestionResponse(
    /** v2.4: 서버가 문자열 questionId(UUID 등)를 줄 수 있어 String으로 수용 */
    @SerializedName("questionId") val questionId: String? = null,
    @SerializedName("questionNo") val questionNo: Int? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("question") val question: String? = null,
    @SerializedName("prompt") val prompt: String? = null,
    @SerializedName("options") val options: List<String>? = null,
    @SerializedName("choices") val choices: List<String>? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName(value = "answerFormat", alternate = ["answer_format"])
    val answerFormat: String? = null,
    @SerializedName("termHints") val termHints: List<TermHintResponse> = emptyList()
)

data class TermHintResponse(
    @SerializedName("term") val term: String,
    @SerializedName("description") val description: String
)

/** 레거시 오프라인 동기화용 */
data class QuizSubmitRequest(
    @SerializedName("quizAttemptId") val quizAttemptId: String,
    @SerializedName("answers") val answers: List<QuizAnswer>
)

data class MissionSetSubmitRequest(
    @SerializedName("answers") val answers: List<MissionSetAnswerItem>
)

data class MissionSetAnswerItem(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("answer") val answer: String
)

data class QuizAnswer(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("selected") val selected: String
)

data class QuizSubmitResponse(
    @SerializedName("attemptId")
    @JsonAdapter(AttemptIdStringAdapter::class)
    val attemptId: String? = null,
    @SerializedName("setId") val setId: String? = null,
    @SerializedName("missionId") val missionId: String? = null,
    @SerializedName("starLevel") val starLevel: Int? = null,
    @SerializedName("variantNo") val variantNo: Int? = null,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("isReview") val isReview: Boolean? = null,
    @SerializedName("progressApplied") val progressApplied: Boolean? = null,
    @SerializedName("attemptState") val attemptState: String? = null,
    @SerializedName("score") val score: Int? = null,
    @SerializedName("correctCount") val correctCount: Int? = null,
    @SerializedName("questionCount") val questionCount: Int? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("wrongCount") val wrongCount: Int? = null,
    @SerializedName("isPassed") val isPassed: Boolean? = null,
    @SerializedName("isPerfect") val isPerfect: Boolean? = null,
    @SerializedName("isFirstClear") val isFirstClear: Boolean? = null,
    @SerializedName("equippedPetGrade") val equippedPetGrade: String? = null,
    @SerializedName("bonusXp") val bonusXp: Int? = null,
    @SerializedName("bonusReason") val bonusReason: String? = null,
    @SerializedName("xpEarned") val xpEarned: Int? = null,
    @SerializedName("exp") val exp: Int? = null,
    @SerializedName("streakBonusApplied") val streakBonusApplied: Boolean? = null,
    @SerializedName("equippedPetXp") val equippedPetXp: Int? = null,
    @SerializedName("petStage") val petStage: String? = null,
    @SerializedName("petEvolved") val petEvolved: Boolean? = null,
    @SerializedName("streakDays") val streakDays: Int? = null,
    @SerializedName(value = "todaySetCount", alternate = ["todayMissionCount"])
    val todaySetCount: Int? = null,
    @SerializedName("rewards")
    @JsonAdapter(SubmitRewardsListAdapter::class)
    val rewards: List<RewardResponse>? = null,
    @SerializedName("remainingTickets") val remainingTickets: RemainingTicketsResponse? = null,
    @SerializedName("results") val results: List<QuestionResultResponse>? = null,
    @SerializedName("currentLevel") val currentLevel: Int? = null,
    @SerializedName("currentXp") val currentXp: Int? = null,
    @SerializedName("nextLevelXp") val nextLevelXp: Int? = null
)

data class RemainingTicketsResponse(
    @SerializedName("normal") val normal: Int = 0,
)

data class RewardResponse(
    @SerializedName("type") val type: String,
    @SerializedName("ticketType") val ticketType: String?,
    @SerializedName("count") val count: Int,
    @SerializedName("reason") val reason: String?
)

data class QuestionResultResponse(
    @SerializedName("questionId") val questionId: String? = null,
    @SerializedName("isCorrect") val isCorrect: Boolean = false,
    @SerializedName("explanation") val explanation: String = ""
)
