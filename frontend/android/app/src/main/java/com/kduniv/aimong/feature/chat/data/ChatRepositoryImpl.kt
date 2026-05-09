package com.kduniv.aimong.feature.chat.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiResponse
import com.kduniv.aimong.core.network.ChatMessageRequest
import com.kduniv.aimong.core.network.ChatMessageResponse
import com.kduniv.aimong.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : ChatRepository {
    override suspend fun sendChatMessage(body: ChatMessageRequest): ApiResponse<ChatMessageResponse> =
        apiService.sendChatMessage(body)
}
