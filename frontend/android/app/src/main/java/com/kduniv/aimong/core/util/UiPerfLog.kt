package com.kduniv.aimong.core.util

import android.os.SystemClock
import android.util.Log
import com.kduniv.aimong.BuildConfig

/**
 * DEBUG 빌드에서만 UI 체감 지연 측정 로그를 남긴다.
 * Logcat 필터: `UiPerf`
 */
object UiPerfLog {

    private const val TAG = "UiPerf"

    fun mark(label: String): Long {
        if (!BuildConfig.DEBUG) return 0L
        val at = SystemClock.uptimeMillis()
        Log.d(TAG, "mark $label at=$at")
        return at
    }

    fun measureFrom(label: String, startedAt: Long, metricName: String) {
        if (!BuildConfig.DEBUG || startedAt <= 0L) return
        val elapsed = SystemClock.uptimeMillis() - startedAt
        Log.d(TAG, "$metricName=${elapsed}ms ($label)")
    }
}
