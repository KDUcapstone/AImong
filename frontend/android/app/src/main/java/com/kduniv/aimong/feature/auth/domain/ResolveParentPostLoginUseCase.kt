package com.kduniv.aimong.feature.auth.domain

import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.parent.data.ParentRepository
import javax.inject.Inject

enum class ParentPostLoginDestination {
    REGISTER_FIRST_CHILD,
    PARENT_HOME
}

/**
 * Google 로그인 직후 자녀 목록으로 온보딩 vs 대시보드 분기.
 */
class ResolveParentPostLoginUseCase @Inject constructor(
    private val parentRepository: ParentRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<ParentPostLoginDestination> {
        return parentRepository.syncParentChildren().fold(
            onSuccess = { children ->
                if (children.isNotEmpty()) {
                    sessionManager.saveSession("PARENT", 1, "")
                    Result.success(ParentPostLoginDestination.PARENT_HOME)
                } else {
                    Result.success(ParentPostLoginDestination.REGISTER_FIRST_CHILD)
                }
            },
            onFailure = { e -> Result.failure(e) }
        )
    }
}
