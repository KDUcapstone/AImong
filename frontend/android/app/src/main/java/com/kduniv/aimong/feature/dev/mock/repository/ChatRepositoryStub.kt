package com.kduniv.aimong.feature.dev.mock.repository

import android.util.Base64
import com.kduniv.aimong.core.network.ApiResponse
import com.kduniv.aimong.core.network.ChatGeneratedImageDto
import com.kduniv.aimong.core.network.ChatMessageRequest
import com.kduniv.aimong.core.network.ChatMessageResponse
import com.kduniv.aimong.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryStub @Inject constructor() : ChatRepository {
    override suspend fun sendChatMessage(body: ChatMessageRequest): ApiResponse<ChatMessageResponse> {
        val preview = body.message.take(48)
        val imageRequested = body.imageRequested == true
        return ApiResponse(
            success = true,
            data = ChatMessageResponse(
                reply = if (imageRequested) {
                    "Image generated."
                } else {
                    "(목업) 「$preview」에 대해 함께 생각해볼까요? 개인정보는 가려서 이야기하는 습관을 들이면 좋아요."
                },
                remainingCalls = 99,
                hintSuggestion = null,
                sessionId = "stub-session",
                image = if (imageRequested) stubImage() else null,
                remainingImageCalls = if (imageRequested) 4 else null,
            )
        )
    }

    /** 1×1 PNG — 목업 미리보기 */
    private fun stubImage(): ChatGeneratedImageDto {
        val png1x1 = Base64.decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
            Base64.DEFAULT
        )
        return ChatGeneratedImageDto(
            b64Json = Base64.encodeToString(png1x1, Base64.NO_WRAP),
            mimeType = "image/png",
            outputFormat = "png",
            size = "1024x1024",
            quality = "low",
        )
    }
}
