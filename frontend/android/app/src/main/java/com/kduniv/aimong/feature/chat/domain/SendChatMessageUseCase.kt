package com.kduniv.aimong.feature.chat.domain

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.ChatMessageRequest
import com.kduniv.aimong.core.network.ChatMessageResponse
import com.kduniv.aimong.core.privacy.PrivacyRadar
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val apiService: AimongApiService,
    private val privacyRadar: PrivacyRadar
) {
    sealed class Result {
        data class Success(val response: ChatMessageResponse) : Result()
        data class Error(val message: String) : Result()
        data class PrivacyBlocked(val sensitiveRanges: List<IntRange>) : Result()
    }

    suspend operator fun invoke(message: String): Result {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return Result.Error("메시지를 입력해주세요.")
        }
        if (trimmed.length > 200) {
            return Result.Error("메시지는 200자 이하로 입력해주세요.")
        }

        // 입력창과 인덱스를 맞추기 위해 trim 전 본문으로 스캔합니다.
        val sensitiveRanges = privacyRadar.scanSensitiveRangesForChat(message)
        if (sensitiveRanges.isNotEmpty()) {
            return Result.PrivacyBlocked(sensitiveRanges)
        }

        return try {
            val response = apiService.sendChatMessage(
                ChatMessageRequest(message = trimmed, masked = false)
            )
            if (response.success) {
                Result.Success(response.data)
            } else {
                Result.Error(ApiErrorMapper.userMessageForApiError(response.error))
            }
        } catch (e: HttpException) {
            Result.Error(ApiErrorMapper.userMessageForHttpException(e))
        } catch (_: IOException) {
            Result.Error("네트워크 연결을 확인해주세요.")
        } catch (e: Exception) {
            Result.Error(e.message?.takeIf { it.isNotBlank() } ?: "요청을 처리하지 못했습니다.")
        }
    }
}
