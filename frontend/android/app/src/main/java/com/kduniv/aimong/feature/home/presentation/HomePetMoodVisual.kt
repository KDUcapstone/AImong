package com.kduniv.aimong.feature.home.presentation

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.ImageView
import android.widget.TextView

/** 명세 5-4 — 슬픔 단계에 따른 홈 펫 스프라이트 표현 (에셋 없을 때 최소 처리) */
object HomePetMoodVisual {

    fun apply(image: ImageView, emojiFallback: TextView, homeState: HomeState) {
        when (homeState) {
            HomeState.SAD_DEEP -> {
                image.alpha = 0.5f
                image.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                emojiFallback.alpha = 0.5f
            }
            HomeState.SAD_LIGHT -> {
                image.alpha = 0.88f
                image.colorFilter = null
                emojiFallback.alpha = 0.88f
            }
            else -> {
                image.alpha = 1f
                image.colorFilter = null
                emojiFallback.alpha = 1f
            }
        }
    }
}
