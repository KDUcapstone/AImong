package com.kduniv.aimong.feature.gacha.data

import android.content.Context
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.dev.mock.StubPetGachaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 누적 뽑기 횟수 — 확률 레벨 표시용 (BE 전용 API 없을 때 로컬·목업) */
@Singleton
class GachaPullCountStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getPullCount(): Int =
        if (UiMode.useStubNav) StubPetGachaStore.gachaPullCount() else prefs.getInt(KEY_COUNT, 0)

    fun setPullCount(count: Int) {
        val safe = count.coerceAtLeast(0)
        if (UiMode.useStubNav) {
            StubPetGachaStore.setGachaPullCount(safe)
        } else {
            prefs.edit().putInt(KEY_COUNT, safe).apply()
        }
    }

    fun incrementPullCount() {
        setPullCount(getPullCount() + 1)
    }

    companion object {
        private const val PREFS = "aimong_gacha"
        private const val KEY_COUNT = "pull_count"
    }
}
