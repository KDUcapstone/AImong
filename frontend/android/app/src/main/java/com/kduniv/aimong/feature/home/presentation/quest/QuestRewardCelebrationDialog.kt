package com.kduniv.aimong.feature.home.presentation.quest

import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieDrawable
import com.kduniv.aimong.R

object QuestRewardCelebrationDialog {

    fun show(host: Fragment, ui: QuestRewardCelebrationUi) {
        if (!host.isAdded) return
        val ctx = host.requireContext()
        try {
            val dialogView = LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_quest_reward_celebration, null, false)
            val dialog = AlertDialog.Builder(ctx, R.style.TransparentDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialogView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lav_quest_reward).apply {
                setAnimation(R.raw.pet_idle)
                repeatCount = LottieDrawable.INFINITE
                alpha = 0.35f
                playAnimation()
            }

            dialogView.findViewById<TextView>(R.id.tv_quest_reward_subtitle).text =
                ctx.getString(R.string.quest_reward_celebration_subtitle, ui.questTitle)

            val linesContainer = dialogView.findViewById<LinearLayout>(R.id.ll_quest_reward_lines)
            linesContainer.removeAllViews()
            val inflater = LayoutInflater.from(ctx)
            ui.lines.forEach { line ->
                val row = inflater.inflate(R.layout.item_quest_reward_celebration_line, linesContainer, false)
                row.findViewById<ImageView>(R.id.iv_reward_icon).setImageResource(line.iconRes)
                row.findViewById<TextView>(R.id.tv_reward_amount).text = line.amountText
                row.findViewById<TextView>(R.id.tv_reward_label).text = line.labelText
                linesContainer.addView(row)
            }

            val rewardsCard = dialogView.findViewById<View>(R.id.card_quest_rewards)
            rewardsCard.scaleX = 0.85f
            rewardsCard.scaleY = 0.85f
            rewardsCard.alpha = 0f
            rewardsCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(380L)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()

            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_quest_reward_close)
                .setOnClickListener { dialog.dismiss() }

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        } catch (_: Exception) {
            val fallback = ui.lines.joinToString(" · ") { "${it.amountText} ${it.labelText}" }
                .ifBlank { ctx.getString(R.string.quest_reward_generic) }
            Toast.makeText(ctx, fallback, Toast.LENGTH_SHORT).show()
        }
    }
}
