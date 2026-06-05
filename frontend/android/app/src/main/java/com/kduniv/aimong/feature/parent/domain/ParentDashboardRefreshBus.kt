package com.kduniv.aimong.feature.parent.domain

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 자녀 커스텀 퀘스트 완료 요청 등 — 부모 대시보드 실세계 미션 영역 갱신 */
@Singleton
class ParentDashboardRefreshBus @Inject constructor() {

    private val _events = MutableSharedFlow<ParentDashboardRefreshTrigger>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<ParentDashboardRefreshTrigger> = _events.asSharedFlow()

    fun notify(trigger: ParentDashboardRefreshTrigger) {
        _events.tryEmit(trigger)
    }
}

sealed interface ParentDashboardRefreshTrigger {
    /** [childId]가 null이면 현재 선택 자녀 기준으로 갱신 */
    data class CustomQuestsChanged(
        val childId: String? = null,
        val showPendingNotice: Boolean = false,
    ) : ParentDashboardRefreshTrigger
}
