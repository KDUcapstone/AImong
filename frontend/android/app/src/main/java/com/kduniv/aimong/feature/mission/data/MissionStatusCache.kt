package com.kduniv.aimong.feature.mission.data

import android.os.SystemClock
import com.kduniv.aimong.feature.mission.data.model.MissionStatusResponseData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GET /missions/{id}/status 응답 단기 캐시.
 * 홈 난이도 피커 → 검증 → 퀴즈 진입 구간의 중복 호출을 줄인다.
 */
@Singleton
class MissionStatusCache @Inject constructor() {

    private val entries = ConcurrentHashMap<String, CachedEntry>()

    fun get(missionId: String, maxAgeMs: Long = DEFAULT_TTL_MS): MissionStatusResponseData? {
        if (missionId.isBlank()) return null
        val entry = entries[missionId] ?: return null
        if (SystemClock.elapsedRealtime() - entry.fetchedAtMs > maxAgeMs) {
            entries.remove(missionId)
            return null
        }
        return entry.status
    }

    fun put(missionId: String, status: MissionStatusResponseData) {
        if (missionId.isBlank()) return
        entries[missionId] = CachedEntry(status, SystemClock.elapsedRealtime())
    }

    fun invalidate(missionId: String) {
        if (missionId.isNotBlank()) entries.remove(missionId)
    }

    fun clear() {
        entries.clear()
    }

    private data class CachedEntry(
        val status: MissionStatusResponseData,
        val fetchedAtMs: Long,
    )

    companion object {
        const val DEFAULT_TTL_MS = 45_000L
    }
}
