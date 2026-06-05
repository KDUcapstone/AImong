package com.kduniv.aimong.feature.chat.domain

import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.ChatMessageRequest
import com.kduniv.aimong.core.network.ChatMessageResponse
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.core.privacy.PrivacyRadar
import com.kduniv.aimong.feature.chat.domain.repository.ChatRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val privacyRadar: PrivacyRadar
) {
    sealed class Result {
        data class Success(
            val response: ChatMessageResponse,
            /** 말풍선에 표시할 내 메시지(마스킹 반영). */
            val sentMessage: String
        ) : Result()

        data class Error(val message: String) : Result()
        data class PrivacyBlocked(val sensitiveRanges: List<IntRange>) : Result()
    }

    suspend operator fun invoke(
        message: String,
        imageRequested: Boolean = false,
        sessionId: String? = null,
    ): Result {
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

        return sendRequest(
            message = trimmed,
            masked = false,
            imageRequested = imageRequested,
            sessionId = sessionId,
            displayMessage = trimmed,
        )
    }

    /** 사용자가 '가리고 보내기'를 선택한 뒤 — [maskForChatSend] 후 `masked=true` 전송. */
    suspend fun sendMasked(
        message: String,
        imageRequested: Boolean = false,
        sessionId: String? = null,
    ): Result {
        val trimmedFull = message.trim()
        if (trimmedFull.isEmpty()) {
            return Result.Error("메시지를 입력해주세요.")
        }
        if (message.length > 200) {
            return Result.Error("메시지는 200자 이하로 입력해주세요.")
        }

        val (masked, _) = privacyRadar.maskForChatSend(message)
        val toSend = masked.trim()
        if (toSend.isEmpty()) {
            return Result.Error("보낼 내용이 없어요. 문장을 조금 더 적어 주세요.")
        }

        return sendRequest(
            message = toSend,
            masked = true,
            imageRequested = imageRequested,
            sessionId = sessionId,
            displayMessage = toSend,
        )
    }

    private suspend fun sendRequest(
        message: String,
        masked: Boolean,
        imageRequested: Boolean,
        sessionId: String?,
        displayMessage: String,
    ): Result {
        return try {
            val data = chatRepository.sendChatMessage(
                ChatMessageRequest(
                    message = message,
                    masked = masked,
                    sessionId = sessionId?.takeIf { it.isNotBlank() },
                    imageRequested = if (imageRequested) true else null,
                )
            ).toResult().getOrThrow()
            Result.Success(data, displayMessage)
        } catch (e: HttpException) {
            Result.Error(ApiErrorMapper.userMessageForChatHttpException(e, imageRequested))
        } catch (_: IOException) {
            Result.Error("네트워크 연결을 확인해주세요.")
        } catch (e: Exception) {
            Result.Error(e.message?.takeIf { it.isNotBlank() } ?: "요청을 처리하지 못했습니다.")
        }
    }
}
