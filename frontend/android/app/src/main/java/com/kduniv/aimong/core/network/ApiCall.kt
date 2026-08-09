package com.kduniv.aimong.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> runApi(block: suspend () -> ApiResponse<T>): Result<T> {
    return try {
        block().toResult()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (_: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** Retrofit 호출을 [Result]로 감쌀 때 코루틴 취소는 실패로 변환하지 않는다. */
suspend fun <T> runApiCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (_: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
