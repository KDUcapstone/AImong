package com.kduniv.aimong.feature.home.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/** GET /home 의 `data` 페이로드 (CHILD JWT) */
data class HomeScreenData(
    @SerializedName("serverDate") val serverDate: String? = null,
    @SerializedName("topStatus") val topStatus: TopStatusDto,
    @SerializedName("profile") val profile: ProfileDto,
    @SerializedName("equippedPet") val equippedPet: EquippedPetDto? = null,
    @SerializedName("missionSummary") val missionSummary: MissionSummaryDto,
    @SerializedName("streak") val streak: StreakDto,
    @SerializedName("dailyQuestSummary") val dailyQuestSummary: DailyQuestSummaryDto,
    @SerializedName("returnReward") val returnReward: ReturnRewardDto,
    @SerializedName("tickets") val tickets: TicketsDto
)

data class TopStatusDto(
    /** BE 가이드: 첫 값은 shieldCount — JSON 키가 heartCount 또는 shieldCount */
    @SerializedName(value = "heartCount", alternate = ["shieldCount"])
    val heartCount: Int = 0,
    /** profile.totalXp 와 동일한 요약값 — xp 또는 totalXp */
    @SerializedName(value = "xp", alternate = ["totalXp"])
    val xp: Int = 0,
    @SerializedName(value = "ticketCount", alternate = ["totalTicketCount"])
    val ticketCount: Int = 0,
    @SerializedName("streakDays")
    val streakDays: Int = 0
)

data class ProfileDto(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImageType") val profileImageType: String,
    @SerializedName("totalXp") val totalXp: Int = 0,
    @SerializedName("todayXp") val todayXp: Int = 0,
    @SerializedName("weeklyXp") val weeklyXp: Int = 0
)

data class EquippedPetDto(
    @SerializedName("id") val id: String,
    @SerializedName("petType") val petType: String,
    @SerializedName("grade") val grade: String,
    @SerializedName("xp") val xp: Int = 0,
    @SerializedName("stage") val stage: String,
    @SerializedName("crownUnlocked") val crownUnlocked: Boolean = false,
    @SerializedName("crownType") val crownType: String? = null
)

data class MissionSummaryDto(
    @SerializedName("todayCompletedCount") val todayCompletedCount: Int = 0,
    @SerializedName("todayTargetCount") val todayTargetCount: Int = 0,
    @SerializedName("canStartMission") val canStartMission: Boolean = false,
    @SerializedName("recommendedMission") val recommendedMission: RecommendedMissionDto? = null
)

data class RecommendedMissionDto(
    @SerializedName(value = "id", alternate = ["missionId"])
    val id: String,
    @SerializedName("stage") val stage: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("isReviewable") val isReviewable: Boolean = false
)

data class StreakDto(
    @SerializedName("continuousDays") val continuousDays: Int = 0,
    @SerializedName("lastCompletedDate") val lastCompletedDate: String? = null,
    @SerializedName("todayMissionCount") val todayMissionCount: Int = 0,
    @SerializedName("shieldCount") val shieldCount: Int = 0,
    /** 확장 필드 — 구조 미정 시 무시 */
    @SerializedName("partner") val partner: JsonElement? = null
)

data class DailyQuestSummaryDto(
    @SerializedName("completedCount") val completedCount: Int = 0,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("claimableCount") val claimableCount: Int = 0,
    @SerializedName("quests") val quests: List<DailyQuestItemDto> = emptyList()
)

data class DailyQuestItemDto(
    @SerializedName("questType") val questType: String,
    @SerializedName("label") val label: String,
    @SerializedName("claimType") val claimType: String,
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("rewardClaimed") val rewardClaimed: Boolean = false,
    @SerializedName("progress") val progress: QuestProgressDto
)

data class QuestProgressDto(
    @SerializedName("current") val current: Int = 0,
    @SerializedName("required") val required: Int = 0
)

data class ReturnRewardDto(
    @SerializedName("hasReward") val hasReward: Boolean = false
)

data class TicketsDto(
    @SerializedName("normal") val normal: Int = 0,
    @SerializedName("rare") val rare: Int = 0,
    @SerializedName("epic") val epic: Int = 0
)
