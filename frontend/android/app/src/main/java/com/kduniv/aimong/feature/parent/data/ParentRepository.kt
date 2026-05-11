package com.kduniv.aimong.feature.parent.data

import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentPrivacyLogResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import kotlinx.coroutines.flow.Flow

interface ParentRepository {
    /** Firebase(PARENT)로 서버에서 자녀 목록을 받아 로컬 JSON에 저장. */
    suspend fun syncParentChildren(): Result<List<ParentChildItem>>

    /** 자녀 코드 재발급 (기존 세션 만료됨) */
    suspend fun regenerateChildCode(childId: String): Result<String>

    fun observeCachedParentChildren(): Flow<List<ParentChildItem>>

    suspend fun getChildSummary(childId: String): Result<ParentChildSummaryResponseData>
    suspend fun getWeeklyStats(childId: String): Result<ParentWeeklyStatsResponseData>
    suspend fun getPrivacyLog(childId: String, page: Int = 0, size: Int = 20): Result<ParentPrivacyLogResponseData>
    suspend fun getWeakPoints(childId: String, page: Int = 0, size: Int = 20): Result<ParentWeakPointsResponseData>
}
