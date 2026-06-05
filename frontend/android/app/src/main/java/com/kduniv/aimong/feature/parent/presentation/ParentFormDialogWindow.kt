package com.kduniv.aimong.feature.parent.presentation

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.view.WindowManager
import com.kduniv.aimong.core.ui.CelebrationDialogWindow

/** 부모 대시보드 폼 다이얼로그 — 카드형·320dp 너비 통일 */
object ParentFormDialogWindow {

    fun apply(dialog: Dialog, context: Context, dimAmount: Float = 0.45f) {
        dialog.window?.apply {
            setLayout(CelebrationDialogWindow.widthPx(context), ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { this.dimAmount = dimAmount }
        }
    }
}
