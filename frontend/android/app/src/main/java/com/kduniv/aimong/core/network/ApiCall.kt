package com.kduniv.aimong.core.network

import retrofit2.HttpException
import java.io.IOException

suspend fun <T> runApi(block: suspend () -> ApiResponse<T>): Result<T> {
    return try {
        val response = block()
        if (response.success) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
        }
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (_: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
