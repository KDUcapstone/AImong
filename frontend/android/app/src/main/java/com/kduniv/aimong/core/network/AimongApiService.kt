package com.kduniv.aimong.core.network

import com.kduniv.aimong.feature.home.data.model.BootstrapResponseData
import com.kduniv.aimong.feature.home.data.model.EnergyAddRequest
import com.kduniv.aimong.feature.home.data.model.EnergyAddResponseData
import com.kduniv.aimong.feature.home.data.model.EnergyStateData
import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.data.model.StreakCalendarData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardClaimResponseData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardCheckResponseData
import com.kduniv.aimong.feature.mission.data.model.MissionsMapResponseData
import com.kduniv.aimong.feature.parent.data.model.CancelParentCustomQuestResponseData
import com.kduniv.aimong.feature.parent.data.model.ConfirmParentCustomQuestResponseData
import com.kduniv.aimong.feature.parent.data.model.CreateParentCustomQuestRequest
import com.kduniv.aimong.feature.parent.data.model.CreateParentCustomQuestResponseData
import com.kduniv.aimong.feature.parent.data.model.CreateParentStageRewardRequest
import com.kduniv.aimong.feature.parent.data.model.CreateParentStageRewardResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentCustomQuestListResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentStageRewardsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.PatchParentStageRewardRequest
import com.kduniv.aimong.feature.parent.data.model.PatchParentStageRewardResponseData
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportRequest
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportResponseData
import com.kduniv.aimong.feature.mission.data.model.MissionStatusResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptReviveRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptReviveResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckResponseData
import com.kduniv.aimong.feature.quiz.data.model.QuizQuestionsResponse
import com.kduniv.aimong.core.network.model.ChildLoginRequest
import com.kduniv.aimong.core.network.model.ChildLoginResponse
import com.kduniv.aimong.core.network.model.ChildLogoutData
import com.kduniv.aimong.core.network.model.ChildMeData
import com.kduniv.aimong.core.network.model.DeletedFlagData
import com.kduniv.aimong.core.network.model.NotificationSettingsData
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest
import com.kduniv.aimong.core.network.model.ParentAccountDeleteRequest
import com.kduniv.aimong.core.network.model.ParentChildDetailData
import com.kduniv.aimong.core.network.model.ParentChildPatchResponseData
import com.kduniv.aimong.core.network.model.ParentMeData
import com.kduniv.aimong.core.network.model.ParentWithdrawData
import com.kduniv.aimong.core.network.model.PatchParentChildRequest
import com.kduniv.aimong.core.network.model.ParentRegisterRequest
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.feature.quest.data.model.DailyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestClaimRequest
import com.kduniv.aimong.feature.quest.data.model.QuestClaimResponseData
import com.kduniv.aimong.feature.quest.data.model.WeeklyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.AchievementsResponseData
import com.kduniv.aimong.feature.quest.data.model.ChildCustomQuestCompleteResponseData
import com.kduniv.aimong.feature.quest.data.model.ChildCustomQuestListResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetSubmitRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionSetReportResponseData
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitResponse
import com.kduniv.aimong.core.network.model.ParentChildrenResponseData
import com.kduniv.aimong.core.network.model.ParentFcmTokenRequest
import com.kduniv.aimong.core.network.model.ParentFcmTokenResponse
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
import com.kduniv.aimong.feature.streak.data.model.StreakShieldPurchaseRequest
import com.kduniv.aimong.feature.streak.data.model.StreakShieldPurchaseResponseData
import com.kduniv.aimong.feature.streak.data.model.StreakStatusData
import com.kduniv.aimong.feature.wallet.data.model.GearAddRequest
import com.kduniv.aimong.feature.wallet.data.model.GearAddResponseData
import com.kduniv.aimong.feature.wallet.data.model.WalletResponseData
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
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

    /** 부모 FCM 해제 — 로그아웃·기기 변경 시 (PARENT) */
    @DELETE("parent/fcm-token")
    suspend fun deleteParentFcmToken(
        @Header("Authorization") authorization: String,
    ): ApiResponse<DeletedFlagData>

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
    ): ApiResponse<ParentChildDetailData>

    /** 부모 계정 정보 — Firebase ID 토큰 (PARENT) */
    @GET("parent/me")
    suspend fun getParentMe(
        @Header("Authorization") authorization: String
    ): ApiResponse<ParentMeData>

    /** 둘째 이상 자녀 추가 — 본문은 최초 온보딩과 동일 */
    @POST("parent/children")
    suspend fun addParentChild(
        @Header("Authorization") authorization: String,
        @Body body: ParentRegisterRequest
    ): ApiResponse<ParentRegisterResponse>

    /** 자녀 등록 완료된 부모용 - 연결 코드 재발급 */
    @PUT("parent/child/{childId}/regenerate-code")
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
    suspend fun getChildMe(): ApiResponse<ChildMeData>

    /** 자녀 로그아웃 — `AuthInterceptor`가 세션 JWT(CHILD) 부착 */
    @POST("child/logout")
    suspend fun childLogout(): ApiResponse<ChildLogoutData>

    /** 자녀 FCM 토큰 등록·갱신 — `AuthInterceptor`가 세션 JWT(CHILD) 부착 */
    @POST("child/fcm-token")
    suspend fun registerChildFcmToken(
        @Body body: ParentFcmTokenRequest
    ): ApiResponse<ParentFcmTokenResponse>

    @DELETE("child/fcm-token")
    suspend fun deleteChildFcmToken(): ApiResponse<DeletedFlagData>

    /** 부모 로그아웃 — BE FCM 해제, FE는 Firebase signOut */
    @POST("parent/logout")
    suspend fun parentLogout(
        @Header("Authorization") authorization: String
    ): ApiResponse<ChildLogoutData>

    @PATCH("parent/children/{childId}")
    suspend fun patchParentChild(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body body: PatchParentChildRequest
    ): ApiResponse<ParentChildPatchResponseData>

    @DELETE("parent/children/{childId}")
    suspend fun deleteParentChild(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): ApiResponse<DeletedFlagData>

    @HTTP(method = "DELETE", path = "parent/account", hasBody = true)
    suspend fun deleteParentAccount(
        @Header("Authorization") authorization: String,
        @Body body: ParentAccountDeleteRequest
    ): ApiResponse<ParentWithdrawData>

    @GET("notification/settings")
    suspend fun getNotificationSettings(): ApiResponse<NotificationSettingsData>

    @GET("notification/settings")
    suspend fun getNotificationSettingsWithAuth(
        @Header("Authorization") authorization: String,
    ): ApiResponse<NotificationSettingsData>

    @PATCH("notification/settings")
    suspend fun patchNotificationSettings(
        @Body body: NotificationSettingsPatchRequest
    ): ApiResponse<NotificationSettingsData>

    @PATCH("notification/settings")
    suspend fun patchNotificationSettingsWithAuth(
        @Header("Authorization") authorization: String,
        @Body body: NotificationSettingsPatchRequest,
    ): ApiResponse<NotificationSettingsData>

    /** 앱 부팅 시 초기 상태 — Authorization 선택(없으면 게스트). */
    @GET("app/bootstrap")
    suspend fun getBootstrap(): ApiResponse<BootstrapResponseData>

    @GET("home")
    suspend fun getHome(): ApiResponse<HomeScreenData>

    @GET("home/streak-calendar")
    suspend fun getStreakCalendar(
        @Query("yearMonth") yearMonth: String? = null
    ): ApiResponse<StreakCalendarData>

    @GET("energy")
    suspend fun getEnergy(): ApiResponse<EnergyStateData>

    @POST("energy/add")
    suspend fun addEnergy(@Body body: EnergyAddRequest): ApiResponse<EnergyAddResponseData>

    @GET("wallet")
    suspend fun getWallet(): ApiResponse<WalletResponseData>

    @POST("wallet/add")
    suspend fun addGear(@Body body: GearAddRequest): ApiResponse<GearAddResponseData>

    @GET("quests/daily")
    suspend fun getDailyQuests(): ApiResponse<DailyQuestsResponseData>

    @GET("quests/weekly")
    suspend fun getWeeklyQuests(): ApiResponse<WeeklyQuestsResponseData>

    @POST("quests/claim")
    suspend fun claimQuest(
        @Body body: QuestClaimRequest
    ): ApiResponse<QuestClaimResponseData>

    @GET("child/custom-quests")
    suspend fun getChildCustomQuests(): ApiResponse<ChildCustomQuestListResponseData>

    @POST("child/custom-quests/{questId}/complete")
    suspend fun completeChildCustomQuest(
        @Path("questId") questId: String
    ): ApiResponse<ChildCustomQuestCompleteResponseData>

    /** 단계별 보상 — 부모 약속 + 기본 기어·티켓 */
    @GET("child/stage-rewards")
    suspend fun getChildStageRewards(): ApiResponse<com.kduniv.aimong.feature.home.data.model.ChildStageRewardsResponseData>

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

    /** v2.5: 세트 제출 결과 리포트 조회 */
    @GET("mission-sets/{setId}/report")
    suspend fun getMissionSetReport(
        @Path("setId") setId: String
    ): ApiResponse<MissionSetReportResponseData>

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

    /** v2.7: 하트 0 — 톱니바퀴로 1회 부활 */
    @POST("mission-attempts/{attemptId}/revive")
    suspend fun reviveMissionAttempt(
        @Path("attemptId") attemptId: String,
        @Body body: MissionAttemptReviveRequest
    ): ApiResponse<MissionAttemptReviveResponseData>

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

    @GET("parent/child/{childId}/weak-points")
    suspend fun getParentChildWeakPoints(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): ApiResponse<ParentWeakPointsResponseData>

    @GET("parent/children/{childId}/custom-quests")
    suspend fun getParentCustomQuests(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Query("status") status: String? = "ACTIVE,PENDING_CONFIRM",
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): ApiResponse<ParentCustomQuestListResponseData>

    @POST("parent/children/{childId}/custom-quests")
    suspend fun createParentCustomQuest(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body body: CreateParentCustomQuestRequest
    ): ApiResponse<CreateParentCustomQuestResponseData>

    @PATCH("parent/custom-quests/{questId}/confirm")
    suspend fun confirmParentCustomQuest(
        @Header("Authorization") authorization: String,
        @Path("questId") questId: String
    ): ApiResponse<ConfirmParentCustomQuestResponseData>

    @DELETE("parent/custom-quests/{questId}")
    suspend fun cancelParentCustomQuest(
        @Header("Authorization") authorization: String,
        @Path("questId") questId: String
    ): ApiResponse<CancelParentCustomQuestResponseData>

    @GET("parent/children/{childId}/stage-rewards")
    suspend fun getParentStageRewards(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): ApiResponse<ParentStageRewardsResponseData>

    @POST("parent/children/{childId}/stage-rewards")
    suspend fun createParentStageReward(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body body: CreateParentStageRewardRequest
    ): ApiResponse<CreateParentStageRewardResponseData>

    @PATCH("parent/children/{childId}/stage-rewards/{stageNumber}")
    suspend fun patchParentStageReward(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Path("stageNumber") stageNumber: Int,
        @Body body: PatchParentStageRewardRequest
    ): ApiResponse<PatchParentStageRewardResponseData>

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

    @POST("streak/shields/purchase")
    suspend fun purchaseStreakShield(
        @Body body: StreakShieldPurchaseRequest
    ): ApiResponse<StreakShieldPurchaseResponseData>

    @POST("streak/shields/use")
    suspend fun useStreakShield(): ApiResponse<com.kduniv.aimong.feature.streak.data.model.StreakShieldUseResponseData>
}

data class ChatMessageRequest(
    val message: String,
    val masked: Boolean,
    val sessionId: String? = null,
    val imageRequested: Boolean? = null,
)

data class ChatGeneratedImageDto(
    val b64Json: String,
    val mimeType: String = "image/png",
    val outputFormat: String? = null,
    val size: String? = null,
    val quality: String? = null,
)

data class ChatMessageResponse(
    val reply: String,
    val remainingCalls: Int,
    val hintSuggestion: String? = null,
    val sessionId: String? = null,
    val sessionExpiresAt: String? = null,
    val image: ChatGeneratedImageDto? = null,
    val remainingImageCalls: Int? = null,
)
