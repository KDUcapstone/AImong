package com.kduniv.aimong.core.network

import com.kduniv.aimong.feature.mission.data.model.MissionListResponse
import com.kduniv.aimong.feature.quiz.data.model.QuizQuestionsResponse
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitRequest
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AimongApiService {
    // MISSION
    @GET("missions")
    suspend fun getMissions(): ApiResponse<MissionListResponse>

    @GET("missions/{missionId}/questions")
    suspend fun getQuestions(
        @Path("missionId") missionId: String
    ): ApiResponse<QuizQuestionsResponse>

    @POST("missions/{missionId}/submit")
    suspend fun submitQuiz(
        @Path("missionId") missionId: String,
        @Body request: QuizSubmitRequest
    ): ApiResponse<QuizSubmitResponse>

    // CHAT
    @POST("chat/send")
    suspend fun sendChatMessage(
        @Body request: ChatMessageRequest
    ): ApiResponse<ChatMessageResponse>
}

data class ChatMessageRequest(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessageResponse(
    val reply: String,
    val conversationId: String,
    val xpEarned: Int? = 0
)
