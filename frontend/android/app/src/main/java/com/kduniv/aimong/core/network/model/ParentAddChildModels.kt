package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

/** POST /parent/children request */
data class ParentAddChildRequest(
    @SerializedName("nickname") val nickname: String
)

