package com.kduniv.aimong.feature.parent.data

import com.kduniv.aimong.core.network.model.ParentChildDetailData
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentMeData
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.core.network.model.PatchParentChildRequest
import com.kduniv.aimong.core.network.model.ParentChildPatchResponseData
import com.kduniv.aimong.feature.parent.data.model.CreateParentCustomQuestRequest
import com.kduniv.aimong.feature.parent.data.model.CreateParentCustomQuestResponseData
import com.kduniv.aimong.feature.parent.data.model.CreateParentStageRewardRequest
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentCustomQuestListResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentStageRewardsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import kotlinx.coroutines.flow.Flow

interface ParentRepository {
    /** Firebase(PARENT)로 서버에서 자녀 목록을 받아 로컬 JSON에 저장. */
    suspend fun syncParentChildren(): Result<List<ParentChildItem>>

    /** 단일 자녀 상세 조회 (대시보드 진입 전 상태 확인/연동 확인 포함) */
    suspend fun getParentChildDetail(childId: String): Result<ParentChildDetailData>

    /** 부모 계정 정보 조회 */
    suspend fun getParentMe(): Result<ParentMeData>

    /** 둘째 이상 자녀 추가 */
    suspend fun addParentChild(nickname: String): Result<ParentRegisterResponse>

    /** 자녀 코드 재발급 (기존 세션 만료됨) */
    suspend fun regenerateChildCode(childId: String): Result<String>

    fun observeCachedParentChildren(): Flow<List<ParentChildItem>>

    suspend fun getChildSummary(childId: String): Result<ParentChildSummaryResponseData>
    suspend fun getWeeklyStats(childId: String): Result<ParentWeeklyStatsResponseData>
    suspend fun getWeakPoints(childId: String, page: Int = 0, size: Int = 20): Result<ParentWeakPointsResponseData>

    suspend fun getCustomQuests(
        childId: String,
        status: String = "ACTIVE,PENDING_CONFIRM",
        page: Int? = null,
        size: Int? = null
    ): Result<ParentCustomQuestListResponseData>

    suspend fun createCustomQuest(
        childId: String,
        body: CreateParentCustomQuestRequest
    ): Result<CreateParentCustomQuestResponseData>

    suspend fun confirmCustomQuest(questId: String): Result<Unit>

    suspend fun cancelCustomQuest(questId: String): Result<Unit>

    suspend fun getStageRewards(childId: String): Result<ParentStageRewardsResponseData>

    suspend fun saveStageReward(
        childId: String,
        stageNumber: Int,
        rewardText: String,
        hasExistingReward: Boolean
    ): Result<Unit>

    /** DELETE /parent/fcm-token — 로그아웃 직전 best-effort */
    suspend fun deleteParentFcmToken(firebaseIdToken: String): Result<Unit>

    /** POST /parent/logout — Firebase ID 토큰 */
    suspend fun parentLogout(firebaseIdToken: String): Result<Unit>

    suspend fun patchParentChild(childId: String, body: PatchParentChildRequest): Result<ParentChildPatchResponseData>

    suspend fun deleteParentChild(childId: String): Result<Unit>

    /** `confirm: true` 본문 — 호출 전 UI 확인 필수 */
    suspend fun deleteParentAccount(firebaseIdToken: String): Result<Unit>
}
