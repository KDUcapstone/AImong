package com.kduniv.aimong.core.ui

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.view.WindowManager
import com.kduniv.aimong.R

/**
 * 축하·보상 등 카드형 [AlertDialog] 너비를 통일한다.
 * 기본 320dp, 좁은 화면에서는 좌우 여백을 남기고 축소한다.
 */
object CelebrationDialogWindow {

    fun apply(dialog: Dialog, context: Context, dimAmount: Float = 0.55f) {
        val widthPx = widthPx(context)
        dialog.window?.apply {
            setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { this.dimAmount = dimAmount }
        }
    }

    fun widthPx(context: Context): Int {
        val resources = context.resources
        val marginPx = (2 * resources.getDimension(R.dimen.dialog_celebration_horizontal_margin)).toInt()
        val preferredPx = resources.getDimensionPixelSize(R.dimen.dialog_celebration_width)
        return minOf(preferredPx, resources.displayMetrics.widthPixels - marginPx)
    }
}
