package com.kduniv.aimong.core.network

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T,
    @SerializedName("error") val error: ApiError? = null,
    @SerializedName("requestId") val requestId: String? = null
)

data class ApiError(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String
)
