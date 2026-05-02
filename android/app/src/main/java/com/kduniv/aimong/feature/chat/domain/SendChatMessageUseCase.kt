package com.kduniv.aimong.feature.chat.domain

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ChatMessageRequest
import com.kduniv.aimong.core.network.ChatMessageResponse
import com.kduniv.aimong.core.privacy.PrivacyRadar
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val apiService: AimongApiService,
    private val privacyRadar: PrivacyRadar
) {
    sealed class Result {
        data class Success(val response: ChatMessageResponse) : Result()
        data class PrivacyViolation(val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(message: String): Result {
        // 1. 개인정보 검사
        if (privacyRadar.checkPrivacy(message)) {
            return Result.PrivacyViolation("메시지에 개인정보(이름, 주소, 전화번호 등)가 포함되어 있어 보낼 수 없어요.")
        }

        // 2. 서버 전송
        return try {
            val response = apiService.sendChatMessage(ChatMessageRequest(message))
            if (response.success) {
                Result.Success(response.data)
            } else {
                Result.Error(response.error?.message ?: "서버 오류가 발생했습니다.")
            }
        } catch (e: Exception) {
            Result.Error("네트워크 연결을 확인해주세요.")
        }
    }
}
