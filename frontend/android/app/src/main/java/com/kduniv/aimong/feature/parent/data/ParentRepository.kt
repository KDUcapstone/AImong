package com.kduniv.aimong.feature.parent.data

import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentChildDetailResponseData
import com.kduniv.aimong.core.network.model.ParentMeResponseData
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import kotlinx.coroutines.flow.Flow

interface ParentRepository {
    /** Firebase(PARENT)로 서버에서 자녀 목록을 받아 로컬 JSON에 저장. */
    suspend fun syncParentChildren(): Result<List<ParentChildItem>>

    /** 단일 자녀 상세 조회 (대시보드 진입 전 상태 확인/연동 확인 포함) */
    suspend fun getParentChildDetail(childId: String): Result<ParentChildDetailResponseData>

    /** 부모 계정 정보 조회 */
    suspend fun getParentMe(): Result<ParentMeResponseData>

    /** 둘째 이상 자녀 추가 */
    suspend fun addChild(nickname: String): Result<ParentRegisterResponse>

    /** 자녀 코드 재발급 (기존 세션 만료됨) */
    suspend fun regenerateChildCode(childId: String): Result<String>

    fun observeCachedParentChildren(): Flow<List<ParentChildItem>>
}
