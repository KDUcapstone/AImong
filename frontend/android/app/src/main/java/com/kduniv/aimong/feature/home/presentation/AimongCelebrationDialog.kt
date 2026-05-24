package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieDrawable
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.CelebrationDialogWindow
import com.kduniv.aimong.feature.gacha.PetArtAssets

object AimongCelebrationDialog {

    fun show(host: Fragment, ui: AimongCelebrationUi) {
        if (!host.isAdded) return
        val ctx = host.requireContext()
        try {
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_aimong_celebration, null, false)
            val dialog = AlertDialog.Builder(ctx, R.style.TransparentDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialogView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lav_aimong_celebration).apply {
                setAnimation(R.raw.pet_idle)
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }

            PetArtAssets.bindEquipped(
                image = dialogView.findViewById(R.id.iv_aimong_pet_sprite),
                emojiFallback = dialogView.findViewById(R.id.tv_aimong_pet_emoji),
                petType = ui.petType,
                stage = "AIMONG",
                grade = ui.grade,
            )

            dialogView.findViewById<android.widget.TextView>(R.id.tv_aimong_title).text =
                ctx.getString(R.string.aimong_celebration_title_fmt, ui.petName)
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_aimong_close)
                .setOnClickListener { dialog.dismiss() }

            dialog.show()
            CelebrationDialogWindow.apply(dialog, ctx)
        } catch (_: Exception) {
            Toast.makeText(
                ctx,
                ctx.getString(R.string.aimong_celebration_title_fmt, ui.petName),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
