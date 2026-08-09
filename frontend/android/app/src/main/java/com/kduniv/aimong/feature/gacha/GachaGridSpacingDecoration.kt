package com.kduniv.aimong.feature.gacha

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** 도감 그리드 카드 사이 간격 */
class GachaGridSpacingDecoration(
    private val spanCount: Int,
    private val spacingPx: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val column = position % spanCount
        outRect.left = spacingPx - column * spacingPx / spanCount
        outRect.right = (column + 1) * spacingPx / spanCount
        if (position >= spanCount) {
            outRect.top = spacingPx
        }
    }
}
