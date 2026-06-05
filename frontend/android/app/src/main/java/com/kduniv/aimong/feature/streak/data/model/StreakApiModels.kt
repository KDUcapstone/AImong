package com.kduniv.aimong.feature.streak.data.model

import com.google.gson.annotations.SerializedName

data class StreakPartnerDto(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("todayCompleted") val todayCompleted: Boolean
)

data class StreakStatusData(
    @SerializedName("continuousDays") val continuousDays: Int,
    @SerializedName("lastCompletedDate") val lastCompletedDate: String?,
    /** v2.0: 오늘 일반 모드 통과 학습 세트 수 (복습/실패 제외) */
    @SerializedName(value = "todaySetCount", alternate = ["todayMissionCount"])
    val todaySetCount: Int = 0,
    @SerializedName("shieldCount") val shieldCount: Int,
    @SerializedName("status") val status: String? = null,
    @SerializedName("recoveryAvailable") val recoveryAvailable: Boolean = false,
    @SerializedName("recoveryDeadlineDate") val recoveryDeadlineDate: String? = null,
    @SerializedName("lastShieldUsedDate") val lastShieldUsedDate: String? = null,
    @SerializedName("partner") val partner: StreakPartnerDto?
)

/** POST /streak/shields/purchase */
data class StreakShieldPurchaseRequest(
    @SerializedName("count") val count: Int = 1
)

data class StreakShieldPurchaseResponseData(
    @SerializedName("shieldCount") val shieldCount: Int,
    @SerializedName("purchasedCount") val purchasedCount: Int? = null,
    @SerializedName("unitCost") val unitCost: Int? = null,
    @SerializedName("gear") val gear: Int? = null,
    @SerializedName("gearBalance") val gearBalance: Int? = null
) {
    fun resolvedGearBalance(): Int? = gearBalance ?: gear
}

/** POST /streak/shields/use — 끊긴 스트릭을 불꽃 방패로 복구 */
data class StreakShieldUseResponseData(
    @SerializedName("continuousDays") val continuousDays: Int? = null,
    @SerializedName("shieldCount") val shieldCount: Int,
    @SerializedName("status") val status: String? = null,
    @SerializedName("used") val used: Boolean = true,
)

