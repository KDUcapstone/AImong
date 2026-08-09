package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.kduniv.aimong.R

/** 홈·목업 공통 — 테마 맞춘 펫 통계 바텀시트 */
object PetStatsSheetPresenter {

    fun show(host: Fragment, state: HomeUiState) {
        if (!host.isAdded) return
        val ctx = host.requireContext()
        val dialog = BottomSheetDialog(ctx, R.style.AimongBottomSheetDialogTheme)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val root = LayoutInflater.from(ctx).inflate(R.layout.bottomsheet_pet_stats, null, false)
        PetStatsSheetUi.bind(
            root = root,
            state = state,
            petNameFallback = ctx.getString(R.string.home_pet_name_default),
        )
        root.findViewById<MaterialButton>(R.id.btn_pet_sheet_close).setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(root)
        dialog.setOnShowListener {
            dialog.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)?.apply {
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_energy_bottom_sheet)
            }
        }
        dialog.show()
    }
}
