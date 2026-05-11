package com.kduniv.aimong.core.network

import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.data.model.StreakCalendarData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardClaimResponseData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardCheckResponseData
import com.kduniv.aimong.feature.mission.data.model.MissionsMapResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentPrivacyLogResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportRequest
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportResponseData
import com.kduniv.aimong.feature.mission.data.model.MissionStatusResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckResponseData
import com.kduniv.aimong.feature.quiz.data.model.QuizQuestionsResponse
import com.kduniv.aimong.core.network.model.ChildLoginRequest
import com.kduniv.aimong.core.network.model.ParentRegisterRequest
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.feature.quest.data.model.DailyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestClaimRequest
import com.kduniv.aimong.feature.quest.data.model.QuestClaimResponseData
import com.kduniv.aimong.feature.quest.data.model.WeeklyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.AchievementsResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetSubmitRequest
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitResponse
import com.kduniv.aimong.core.network.model.ChildLoginResponse
import com.kduniv.aimong.core.network.model.ChildLogoutResponse
import com.kduniv.aimong.core.network.model.ChildMeResponseData
import com.kduniv.aimong.core.network.model.NotificationSettingsRequest
import com.kduniv.aimong.core.network.model.NotificationSettingsResponseData
import com.kduniv.aimong.core.network.model.ParentAddChildRequest
import com.kduniv.aimong.core.network.model.ParentChildDetailResponseData
import com.kduniv.aimong.core.network.model.ParentChildrenResponseData
import com.kduniv.aimong.core.network.model.ParentFcmTokenRequest
import com.kduniv.aimong.core.network.model.ParentFcmTokenResponse
import com.kduniv.aimong.core.network.model.ParentMeResponseData
import com.kduniv.aimong.core.network.model.PrivacyEventRequest
import com.kduniv.aimong.core.network.model.PrivacyEventResponseData
import com.kduniv.aimong.core.network.model.RegenerateCodeResponse
import com.kduniv.aimong.feature.gacha.data.model.GachaExchangeData
import com.kduniv.aimong.feature.gacha.data.model.GachaExchangeRequest
import com.kduniv.aimong.feature.gacha.data.model.GachaFragmentsData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullRequest
import com.kduniv.aimong.feature.pet.data.model.PetEquipData
import com.kduniv.aimong.feature.pet.data.model.PetEquipRequest
import com.kduniv.aimong.feature.pet.data.model.PetListData
import com.kduniv.aimong.feature.streak.data.model.StreakStatusData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.PUT
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

    /** 부모 단일 자녀 상세 — Firebase ID 토큰 (PARENT) */
    @GET("parent/children/{childId}")
    suspend fun getParentChildDetail(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): ApiResponse<ParentChildDetailResponseData>

    /** 부모 계정 정보 — Firebase ID 토큰 (PARENT) */
    @GET("parent/me")
    suspend fun getParentMe(
        @Header("Authorization") authorization: String
    ): ApiResponse<ParentMeResponseData>

    /** 둘째 이상 자녀 추가 — Firebase ID 토큰 (PARENT) */
    @POST("parent/children")
    suspend fun addParentChild(
        @Header("Authorization") authorization: String,
        @Body body: ParentAddChildRequest
    ): ApiResponse<ParentRegisterResponse>

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

    /** 자녀 세션 확인 — `AuthInterceptor`가 세션 JWT(CHILD) 부착 */
    @GET("child/me")
    suspend fun getChildMe(): ApiResponse<ChildMeResponseData>

    /** 자녀 로그아웃 — `AuthInterceptor`가 세션 JWT(CHILD) 부착 */
    @POST("child/logout")
    suspend fun childLogout(): ApiResponse<ChildLogoutResponse>

    /** 자녀 FCM 토큰 등록·갱신 — `AuthInterceptor`가 세션 JWT(CHILD) 부착 */
    @POST("child/fcm-token")
    suspend fun registerChildFcmToken(
        @Body body: ParentFcmTokenRequest
    ): ApiResponse<ParentFcmTokenResponse>

    /** 부모 FCM 토큰 해제 — Firebase ID 토큰 (PARENT) */
    @DELETE("parent/fcm-token")
    suspend fun deleteParentFcmToken(
        @Header("Authorization") authorization: String
    ): ApiResponse<ParentFcmTokenResponse>

    /** 자녀 FCM 토큰 해제 — `AuthInterceptor`가 세션 JWT(CHILD) 부착 */
    @DELETE("child/fcm-token")
    suspend fun deleteChildFcmToken(): ApiResponse<ParentFcmTokenResponse>

    /** 알림 설정 조회 — CHILD는 interceptor 사용 */
    @GET("notification/settings")
    suspend fun getNotificationSettings(): ApiResponse<NotificationSettingsResponseData>

    /** 알림 설정 조회 — PARENT는 Firebase ID 토큰 필요 */
    @GET("notification/settings")
    suspend fun getNotificationSettingsParent(
        @Header("Authorization") authorization: String
    ): ApiResponse<NotificationSettingsResponseData>

    /** 알림 설정 변경 — CHILD는 interceptor 사용 */
    @PATCH("notification/settings")
    suspend fun patchNotificationSettings(
        @Body body: NotificationSettingsRequest
    ): ApiResponse<NotificationSettingsResponseData>

    /** 알림 설정 변경 — PARENT는 Firebase ID 토큰 필요 */
    @PATCH("notification/settings")
    suspend fun patchNotificationSettingsParent(
        @Header("Authorization") authorization: String,
        @Body body: NotificationSettingsRequest
    ): ApiResponse<NotificationSettingsResponseData>

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

    // ACHIEVEMENTS (CHILD)
    @GET("achievements")
    suspend fun getAchievements(): ApiResponse<AchievementsResponseData>

    // MISSION / LEARNING (v2.3)
    @GET("missions")
    suspend fun getMissions(): ApiResponse<MissionsMapResponseData>

    /** v2.4: 미션 진입 전 상태 조회 */
    @GET("missions/{missionId}/status")
    suspend fun getMissionStatus(
        @Path("missionId") missionId: String
    ): ApiResponse<MissionStatusResponseData>

    @GET("missions/{missionId}/questions")
    suspend fun getMissionQuestions(
        @Path("missionId") missionId: String,
        @Query("starLevel") starLevel: Int
    ): ApiResponse<QuizQuestionsResponse>

    @GET("mission-sets/{setId}/questions")
    suspend fun getMissionSetQuestions(
        @Path("setId") setId: String
    ): ApiResponse<QuizQuestionsResponse>

    @POST("mission-sets/{setId}/submit")
    suspend fun submitMissionSet(
        @Path("setId") setId: String,
        @Body body: MissionSetSubmitRequest
    ): ApiResponse<QuizSubmitResponse>

    /** v2.4: 문항 단위 채점 */
    @POST("mission-sets/{setId}/check")
    suspend fun checkMissionSetAnswer(
        @Path("setId") setId: String,
        @Body body: MissionSetCheckRequest
    ): ApiResponse<MissionSetCheckResponseData>

    /** v2.4: 진행 중 attempt 복구 */
    @GET("mission-attempts/{attemptId}")
    suspend fun getMissionAttempt(
        @Path("attemptId") attemptId: String
    ): ApiResponse<MissionAttemptResponseData>

    /** v2.4: 중도 이탈 */
    @POST("mission-attempts/{attemptId}/abandon")
    suspend fun abandonMissionAttempt(
        @Path("attemptId") attemptId: String,
        @Body body: MissionAttemptAbandonRequest
    ): ApiResponse<MissionAttemptAbandonResponseData>

    @POST("missions/{missionId}/questions/{questionId}/report")
    suspend fun reportQuestion(
        @Path("missionId") missionId: String,
        @Path("questionId") questionId: String,
        @Body request: QuestionReportRequest
    ): ApiResponse<QuestionReportResponseData>

    // RETURN REWARD (CHILD)
    @GET("return-reward")
    suspend fun getReturnReward(): ApiResponse<ReturnRewardCheckResponseData>

    @POST("return-reward/claim")
    suspend fun claimReturnReward(): ApiResponse<ReturnRewardClaimResponseData>

    // PARENT DASHBOARD (PARENT)
    @GET("parent/child/{childId}/summary")
    suspend fun getParentChildSummary(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): ApiResponse<ParentChildSummaryResponseData>

    @GET("parent/child/{childId}/weekly-stats")
    suspend fun getParentChildWeeklyStats(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): ApiResponse<ParentWeeklyStatsResponseData>

    @GET("parent/child/{childId}/privacy-log")
    suspend fun getParentChildPrivacyLog(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): ApiResponse<ParentPrivacyLogResponseData>

    @GET("parent/child/{childId}/weak-points")
    suspend fun getParentChildWeakPoints(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): ApiResponse<ParentWeakPointsResponseData>

    // CHAT
    @POST("chat/send")
    suspend fun sendChatMessage(
        @Body request: ChatMessageRequest
    ): ApiResponse<ChatMessageResponse>

    @POST("privacy/event")
    suspend fun reportPrivacyEvent(
        @Body body: PrivacyEventRequest
    ): ApiResponse<PrivacyEventResponseData>

    // PET (CHILD)
    @GET("pet")
    suspend fun getPets(): ApiResponse<PetListData>

    @PUT("pet/equip")
    suspend fun equipPet(
        @Body body: PetEquipRequest
    ): ApiResponse<PetEquipData>

    // GACHA (CHILD)
    @POST("gacha/pull")
    suspend fun gachaPull(
        @Body body: GachaPullRequest
    ): ApiResponse<GachaPullData>

    @GET("gacha/fragments")
    suspend fun getGachaFragments(): ApiResponse<GachaFragmentsData>

    @POST("gacha/exchange")
    suspend fun gachaExchange(
        @Body body: GachaExchangeRequest
    ): ApiResponse<GachaExchangeData>

    // STREAK (CHILD)
    @GET("streak")
    suspend fun getStreak(): ApiResponse<StreakStatusData>
}

data class ChatMessageRequest(
    val message: String,
    val masked: Boolean
)

data class ChatMessageResponse(
    val reply: String,
    val remainingCalls: Int,
    val hintSuggestion: String? = null
)
