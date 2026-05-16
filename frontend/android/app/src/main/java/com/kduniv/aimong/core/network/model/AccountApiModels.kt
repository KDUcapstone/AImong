package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

/** GET /child/me 의 `data` */
data class ChildMeData(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImageType") val profileImageType: String,
    @SerializedName("totalXp") val totalXp: Int = 0,
    @SerializedName("hasFcmToken") val hasFcmToken: Boolean? = null,
    @SerializedName("lastActiveAt") val lastActiveAt: String? = null
)

/** POST /child/logout · POST /parent/logout 의 `data` */
data class ChildLogoutData(
    @SerializedName("loggedOut") val loggedOut: Boolean = true
)

/** DELETE …/fcm-token 등 `deleted: true` */
data class DeletedFlagData(
    @SerializedName("deleted") val deleted: Boolean = true
)

/** GET /parent/me 의 `data` */
data class ParentMeData(
    @SerializedName("parentId") val parentId: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("hasFcmToken") val hasFcmToken: Boolean? = null,
    @SerializedName("childrenCount") val childrenCount: Int? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

/** GET /parent/children/{childId} 의 `data` */
data class ParentChildDetailData(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("code") val code: String? = null,
    @SerializedName("profileImageType") val profileImageType: String,
    @SerializedName("totalXp") val totalXp: Int = 0,
    @SerializedName("hasFcmToken") val hasFcmToken: Boolean? = null,
    @SerializedName("lastActiveAt") val lastActiveAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class PatchParentChildRequest(
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImageType") val profileImageType: String? = null
)

data class ParentChildPatchResponseData(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImageType") val profileImageType: String
)

data class ParentAccountDeleteRequest(
    @SerializedName("confirm") val confirm: Boolean
)

data class ParentWithdrawData(
    @SerializedName("withdrawn") val withdrawn: Boolean = true
)

data class NotificationSettingsData(
    @SerializedName("privacyAlertEnabled") val privacyAlertEnabled: Boolean = true,
    @SerializedName("studyReminderEnabled") val studyReminderEnabled: Boolean = true,
    @SerializedName("returnRewardEnabled") val returnRewardEnabled: Boolean = true,
    @SerializedName("questRewardEnabled") val questRewardEnabled: Boolean = true,
    @SerializedName("marketingEnabled") val marketingEnabled: Boolean = false
)

/** PATCH /notification/settings — 전달된 필드만 서버가 반영한다고 가정 */
data class NotificationSettingsPatchRequest(
    @SerializedName("privacyAlertEnabled") val privacyAlertEnabled: Boolean? = null,
    @SerializedName("studyReminderEnabled") val studyReminderEnabled: Boolean? = null,
    @SerializedName("returnRewardEnabled") val returnRewardEnabled: Boolean? = null,
    @SerializedName("questRewardEnabled") val questRewardEnabled: Boolean? = null,
    @SerializedName("marketingEnabled") val marketingEnabled: Boolean? = null
)
