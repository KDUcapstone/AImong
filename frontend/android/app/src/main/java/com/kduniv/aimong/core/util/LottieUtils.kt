package com.kduniv.aimong.core.util

import android.view.View
import com.airbnb.lottie.LottieAnimationView

object LottieUtils {
    /**
     * 특정 뷰에 Lottie 애니메이션을 재생합니다.
     */
    fun playAnimation(
        view: LottieAnimationView,
        animationName: String,
        onComplete: () -> Unit = {}
    ) {
        view.visibility = View.VISIBLE
        view.setAnimation(animationName)
        view.playAnimation()
        
        // 애니메이션이 끝나면 리스너 제거 및 숨김 처리 (단발성)
        view.addAnimatorUpdateListener {
            if (view.progress >= 1f) {
                view.visibility = View.GONE
                onComplete()
            }
        }
    }

    // 파일 이름 정의 (assets/raw 폴더에 파일이 추가되어야 함)
    const val ANIM_CORRECT = "anim_correct.json"
    const val ANIM_WRONG = "anim_wrong.json"
    const val ANIM_SUCCESS = "anim_success.json"
}
