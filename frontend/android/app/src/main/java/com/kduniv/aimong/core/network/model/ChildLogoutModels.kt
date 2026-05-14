package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

/** POST /child/logout 의 data 본문 */
data class ChildLogoutResponse(
    @SerializedName("loggedOut") val loggedOut: Boolean
)

