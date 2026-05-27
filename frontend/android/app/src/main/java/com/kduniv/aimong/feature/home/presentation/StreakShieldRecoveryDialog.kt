package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.CelebrationDialogWindow

object StreakShieldRecoveryDialog {

    fun show(
        host: Fragment,
        ui: StreakShieldRecoveryUi,
        onUseShield: () -> Unit,
        onDismissForNow: () -> Unit,
    ) {
        if (!host.isAdded) return
        val ctx = host.requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_streak_shield_recovery, null, false)
        val dialog = AlertDialog.Builder(ctx, R.style.TransparentDialog)
            .setView(view)
            .setCancelable(true)
            .create()

        view.findViewById<TextView>(R.id.tv_streak_shield_recovery_title).text =
            if (ui.isRecoverable) {
                ctx.getString(R.string.streak_shield_recovery_title_recoverable)
            } else {
                ctx.getString(R.string.streak_shield_recovery_title_broken)
            }
        view.findViewById<TextView>(R.id.tv_streak_shield_recovery_body).text =
            ctx.getString(
                R.string.streak_shield_recovery_body_fmt,
                ui.continuousDays.coerceAtLeast(0),
                ui.shieldCount.coerceAtLeast(0),
            )

        view.findViewById<MaterialButton>(R.id.btn_streak_shield_use).setOnClickListener {
            dialog.dismiss()
            onUseShield()
        }
        view.findViewById<MaterialButton>(R.id.btn_streak_shield_later).setOnClickListener {
            dialog.dismiss()
            onDismissForNow()
        }
        dialog.setOnCancelListener { onDismissForNow() }

        dialog.show()
        CelebrationDialogWindow.apply(dialog, ctx)
    }
}
