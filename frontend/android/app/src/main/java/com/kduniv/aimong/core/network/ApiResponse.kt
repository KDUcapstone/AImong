package com.kduniv.aimong.core.network

// TODO: sealed class — 네트워크 응답 래퍼
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val code: Int, val message: String) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
}
