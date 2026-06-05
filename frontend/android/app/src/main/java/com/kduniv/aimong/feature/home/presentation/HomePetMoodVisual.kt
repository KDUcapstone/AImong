package com.kduniv.aimong.feature.home.presentation

import android.widget.ImageView
import android.widget.TextView

/** 명세 5-4 — 슬픔 단계에 따른 홈 펫 스프라이트 표현 (에셋 없을 때 최소 처리) */
object HomePetMoodVisual {

    fun apply(image: ImageView, emojiFallback: TextView, homeState: HomeState) {
        // 홈 펫은 상태와 무관하게 항상 컬러로 표시한다.
        // (SAD_* 상태 표현은 이름/왕관 영역의 아이콘으로 처리)
        image.alpha = 1f
        image.colorFilter = null
        emojiFallback.alpha = 1f
    }
}
