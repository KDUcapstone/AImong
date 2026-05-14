package com.kduniv.aimong.feature.dev.mock.repository

import com.kduniv.aimong.core.network.ApiResponse
import com.kduniv.aimong.core.network.ChatMessageRequest
import com.kduniv.aimong.core.network.ChatMessageResponse
import com.kduniv.aimong.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryStub @Inject constructor() : ChatRepository {
    override suspend fun sendChatMessage(body: ChatMessageRequest): ApiResponse<ChatMessageResponse> {
        val preview = body.message.take(48)
        return ApiResponse(
            success = true,
            data = ChatMessageResponse(
                reply = "(목업) 「$preview」에 대해 함께 생각해볼까요? 개인정보는 가려서 이야기하는 습관을 들이면 좋아요.",
                remainingCalls = 99,
                hintSuggestion = null
            )
        )
    }
}
