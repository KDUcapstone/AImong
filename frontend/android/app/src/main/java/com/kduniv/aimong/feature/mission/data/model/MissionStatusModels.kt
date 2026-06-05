package com.kduniv.aimong.feature.mission.data.model

import com.google.gson.annotations.SerializedName

/** v2.4: GET /missions/{missionId}/status */
data class MissionStatusResponseData(
    /** v2.4: missionId가 UUID/String일 수 있어 String으로 수용 */
    @SerializedName("missionId") val missionId: String? = null,
    @SerializedName("missionCode") val missionCode: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("isUnlocked") val isUnlocked: Boolean = true,
    @SerializedName("canStartMission") val canStartMission: Boolean = true,
    @SerializedName("energy") val energy: MissionEnergyDto? = null,
    @SerializedName("starLevels") val starLevels: List<MissionStarLevelDto> = emptyList(),
    @SerializedName("inProgressAttempt") val inProgressAttempt: InProgressAttemptDto? = null
)

data class MissionEnergyDto(
    @SerializedName("current") val current: Int = 0,
    @SerializedName("required") val required: Int = 0,
    @SerializedName("maxEnergy") val maxEnergy: Int = 0,
    @SerializedName("nextEnergyRecoverAt") val nextEnergyRecoverAt: String? = null
)

data class MissionStarLevelDto(
    @SerializedName("starLevel") val starLevel: Int,
    @SerializedName("label") val label: String,
    @SerializedName("totalSetCount") val totalSetCount: Int = 0,
    @SerializedName("completedSetCount") val completedSetCount: Int = 0,
    @SerializedName("isPlayable") val isPlayable: Boolean = false,
    @SerializedName("isReviewable") val isReviewable: Boolean = false
)

data class InProgressAttemptDto(
    @SerializedName("attemptId") val attemptId: String,
    /** v2.4: 서버가 문자열 setId를 줄 수 있어 String으로 수용 */
    @SerializedName("setId") val setId: String,
    @SerializedName("starLevel") val starLevel: Int? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null
)

