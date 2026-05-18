package com.kduniv.aimong.feature.home.domain

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 미션·퀘스트·가챠 등으로 홈 데이터가 바뀌었을 때 [HomeViewModel]이 즉시 GET /home·/missions 를 다시 받도록 알린다. */
@Singleton
class ChildHomeRefreshBus @Inject constructor() {

    private val _events = MutableSharedFlow<HomeRefreshTrigger>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<HomeRefreshTrigger> = _events.asSharedFlow()

    fun notify(trigger: HomeRefreshTrigger) {
        _events.tryEmit(trigger)
    }
}

sealed interface HomeRefreshTrigger {
    /** XP·미션 경로·스트릭·에너지·티켓 등 전체 동기화 */
    data object Full : HomeRefreshTrigger

    /** 미션 제출 직후 — 서버 응답 전 칩·펫 바 선반영용 */
    data class MissionCompleted(
        val xpEarned: Int = 0,
        val equippedPetXp: Int = 0,
    ) : HomeRefreshTrigger
}
