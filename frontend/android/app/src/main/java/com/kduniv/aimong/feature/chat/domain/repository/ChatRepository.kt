package com.kduniv.aimong.feature.chat.domain.repository

import com.kduniv.aimong.core.network.ApiResponse
import com.kduniv.aimong.core.network.ChatMessageRequest
import com.kduniv.aimong.core.network.ChatMessageResponse

interface ChatRepository {
    suspend fun sendChatMessage(body: ChatMessageRequest): ApiResponse<ChatMessageResponse>
}
