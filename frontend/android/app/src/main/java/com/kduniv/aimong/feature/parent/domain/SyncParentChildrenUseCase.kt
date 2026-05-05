package com.kduniv.aimong.feature.parent.domain

import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.parent.data.ParentRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 부모 세션일 때 서버에서 자녀 목록을 받아 로컬에 저장(재설치·복구 대비). */
class SyncParentChildrenUseCase @Inject constructor(
    private val parentRepository: ParentRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke() {
        if (sessionManager.userRole.first() != "PARENT") return
        runCatching { parentRepository.syncParentChildren() }
    }
}
