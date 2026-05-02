package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * API 명세서 v1.3 기준 인증 관련 DTO
 */

// 부모 등록 요청
data class ParentRegisterRequest(
    val nickname: String
)

// 부모 등록 응답
data class ParentRegisterResponse(
    val childId: String,
    val nickname: String,
    val code: String,
    val starterTickets: Int
)

// 자녀 로그인 요청
data class ChildLoginRequest(
    val code: String
)

// 자녀 로그인 응답
data class ChildLoginResponse(
    val childId: String,
    val nickname: String,
    val sessionToken: String,
    val profileImageType: String,
    val totalXp: Int
)

// 공통 응답 형식
data class BaseResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String
)
