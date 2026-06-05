package com.kduniv.aimong.feature.onboarding.child

import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.CelebrationDialogWindow

object ChildGachaOnboardingDialogs {

    fun showWelcome(
        host: Fragment,
        ticketCount: Int,
        onStart: () -> Unit,
    ) {
        if (!host.isAdded) return
        val ctx = host.requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_child_gacha_onboarding_welcome, null, false)
        view.findViewById<TextView>(R.id.tv_onboarding_welcome_body).text =
            ctx.getString(R.string.child_onboarding_welcome_body_fmt, ticketCount)
        val dialog = AlertDialog.Builder(ctx, R.style.TransparentDialog)
            .setView(view)
            .setCancelable(false)
            .create()
        view.findViewById<MaterialButton>(R.id.btn_onboarding_welcome_start).setOnClickListener {
            dialog.dismiss()
            onStart()
        }
        dialog.show()
        CelebrationDialogWindow.apply(dialog, ctx)
    }

    fun showNoTickets(host: Fragment) {
        if (!host.isAdded) return
        val ctx = host.requireContext()
        AlertDialog.Builder(ctx, R.style.TransparentDialog)
            .setTitle(R.string.child_onboarding_no_tickets_title)
            .setMessage(R.string.child_onboarding_no_tickets_message)
            .setCancelable(true)
            .setPositiveButton(R.string.child_onboarding_no_tickets_ok, null)
            .create()
            .apply {
                show()
                CelebrationDialogWindow.apply(this, ctx)
            }
    }

    fun showComplete(host: Fragment, onGoHome: () -> Unit) {
        if (!host.isAdded) return
        val ctx = host.requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_child_gacha_onboarding_complete, null, false)
        val dialog = AlertDialog.Builder(ctx, R.style.TransparentDialog)
            .setView(view)
            .setCancelable(false)
            .create()
        view.findViewById<MaterialButton>(R.id.btn_onboarding_complete_home).setOnClickListener {
            dialog.dismiss()
            onGoHome()
        }
        dialog.show()
        CelebrationDialogWindow.apply(dialog, ctx)
    }
}
