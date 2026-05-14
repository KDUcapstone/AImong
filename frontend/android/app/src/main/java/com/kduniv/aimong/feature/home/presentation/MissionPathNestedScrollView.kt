package com.kduniv.aimong.feature.home.presentation

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 * 난이도 인라인 팝업이 열려 있을 때, 팝업 밖을 누르면 닫기 콜백을 호출한다.
 */
class MissionPathNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : NestedScrollView(context, attrs, defStyleAttr) {

    var popupBoundsInScroll: (() -> Rect?)? = null
    var onOutsidePopupAction: (() -> Unit)? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val r = popupBoundsInScroll?.invoke()
            if (r != null) {
                val x = ev.x.toInt()
                val y = ev.y.toInt()
                if (!r.contains(x, y)) {
                    post { onOutsidePopupAction?.invoke() }
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
