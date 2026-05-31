package com.kduniv.aimong.feature.home.presentation

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import com.kduniv.aimong.R

/**
 * 난이도 인라인 팝업이 열려 있을 때, 팝업 밖을 누르면 닫기 콜백을 호출한다.
 * 팝업·난이도 카드 안의 터치는 닫지 않고 자식 뷰로 전달한다.
 */
class MissionPathNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : NestedScrollView(context, attrs, defStyleAttr) {

    var onOutsidePopupAction: (() -> Unit)? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val popup = findOpenDifficultyPopup()
            if (popup != null && !isTouchInsideView(popup, ev)) {
                post { onOutsidePopupAction?.invoke() }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun findOpenDifficultyPopup(): View? {
        val content = getChildAt(0) as? ViewGroup ?: return null
        return content.findViewById(R.id.mission_diff_popup_root)
    }

    private fun isTouchInsideView(view: View, ev: MotionEvent): Boolean {
        if (!view.isShown || view.width <= 0 || view.height <= 0) return false
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val x = ev.rawX
        val y = ev.rawY
        return x >= loc[0] && x < loc[0] + view.width &&
            y >= loc[1] && y < loc[1] + view.height
    }
}
