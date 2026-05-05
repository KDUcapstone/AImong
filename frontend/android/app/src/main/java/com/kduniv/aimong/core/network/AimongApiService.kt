package com.kduniv.aimong.core.network

import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.data.model.StreakCalendarData
import com.kduniv.aimong.feature.mission.data.model.MissionListResponse
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportRequest
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportResponseData
import com.kduniv.aimong.feature.quiz.data.model.QuizQuestionsResponse
import com.kduniv.aimong.core.network.model.ChildLoginRequest
import com.kduniv.aimong.core.network.model.ParentRegisterRequest
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.feature.quest.data.model.DailyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestClaimRequest
import com.kduniv.aimong.feature.quest.data.model.QuestClaimResponseData
import com.kduniv.aimong.feature.quest.data.model.WeeklyQuestsResponseData
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitRequest
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitResponse
import com.kduniv.aimong.core.network.model.ChildLoginResponse
import com.kduniv.aimong.core.network.model.ParentChildrenResponseData
import com.kduniv.aimong.core.network.model.ParentFcmTokenRequest
import com.kduniv.aimong.core.network.model.ParentFcmTokenResponse
import com.kduniv.aimong.core.network.model.RegenerateCodeResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AimongApiService {

    @POST("parent/register")
    suspend fun parentRegister(
        @Header("Authorization") authorization: String,
        @Body body: ParentRegisterRequest
    ): ApiResponse<ParentRegisterResponse>

    /** 부모 FCM 토큰 등록·갱신 — Firebase ID 토큰 (PARENT) */
    @POST("parent/fcm-token")
    suspend fun registerParentFcmToken(
        @Header("Authorization") authorization: String,
        @Body body: ParentFcmTokenRequest
    ): ApiResponse<ParentFcmTokenResponse>

    /** 부모 등록 자녀 목록 — Firebase ID 토큰 (PARENT) */
    @GET("parent/children")
    suspend fun getParentChildren(
        @Header("Authorization") authorization: String
    ): ApiResponse<ParentChildrenResponseData>

    /** 자녀 등록 완료된 부모용 - 연결 코드 재발급 */
    @retrofit2.http.PUT("parent/child/{childId}/regenerate-code")
    suspend fun regenerateChildCode(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): ApiResponse<RegenerateCodeResponse>

    /** 자녀 세션 발급 — 로그인 전에는 Authorization 없음 */
    @POST("child/login")
    suspend fun childLogin(
        @Body body: ChildLoginRequest
    ): ApiResponse<ChildLoginResponse>

    /** 자녀 FCM 토큰 등록·갱신 — `AuthInterceptor`가 세션 JWT(CHILD) 부착 */
    @POST("child/fcm-token")
    suspend fun registerChildFcmToken(
        @Body body: ParentFcmTokenRequest
    ): ApiResponse<ParentFcmTokenResponse>

    @GET("home")
    suspend fun getHome(): ApiResponse<HomeScreenData>

    @GET("home/streak-calendar")
    suspend fun getStreakCalendar(
        @Query("yearMonth") yearMonth: String? = null
    ): ApiResponse<StreakCalendarData>

    @GET("quests/daily")
    suspend fun getDailyQuests(): ApiResponse<DailyQuestsResponseData>

    @GET("quests/weekly")
    suspend fun getWeeklyQuests(): ApiResponse<WeeklyQuestsResponseData>

    @POST("quests/claim")
    suspend fun claimQuest(
        @Body body: QuestClaimRequest
    ): ApiResponse<QuestClaimResponseData>

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

    @POST("missions/{missionId}/questions/{questionId}/report")
    suspend fun reportQuestion(
        @Path("missionId") missionId: String,
        @Path("questionId") questionId: String,
        @Body request: QuestionReportRequest
    ): ApiResponse<QuestionReportResponseData>

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
