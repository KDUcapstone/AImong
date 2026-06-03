package com.kduniv.aimong.feature.home.presentation.quest

import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.CelebrationDialogWindow

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

            val heroIcon = dialogView.findViewById<ImageView>(R.id.iv_quest_reward_hero)
            val primaryLine = ui.lines.firstOrNull()
            heroIcon.setImageResource(primaryLine?.iconRes ?: R.drawable.ic_check_circle)

            val hero = dialogView.findViewById<View>(R.id.layout_quest_reward_hero)
            hero.scaleX = 0.7f
            hero.scaleY = 0.7f
            hero.alpha = 0f
            hero.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(320L)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()

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
            rewardsCard.scaleX = 0.92f
            rewardsCard.scaleY = 0.92f
            rewardsCard.alpha = 0f
            rewardsCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setStartDelay(80L)
                .setDuration(340L)
                .setInterpolator(OvershootInterpolator(1.08f))
                .start()

            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_quest_reward_close)
                .setOnClickListener { dialog.dismiss() }

            dialog.show()
            CelebrationDialogWindow.apply(dialog, ctx, dimAmount = 0.42f)
        } catch (_: Exception) {
            val fallback = ui.lines.joinToString(" · ") { "${it.amountText} ${it.labelText}" }
                .ifBlank { ctx.getString(R.string.quest_reward_generic) }
            Toast.makeText(ctx, fallback, Toast.LENGTH_SHORT).show()
        }
    }
}
