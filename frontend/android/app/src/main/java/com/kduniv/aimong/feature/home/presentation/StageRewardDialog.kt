package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.CelebrationDialogWindow

object StageRewardDialog {

    fun show(host: Fragment, reward: StageRewardUi) {
        if (!host.isAdded) return
        val ctx = host.requireContext()
        try {
            val dialogView = LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_home_stage_reward, null, false)
            val dialog = AlertDialog.Builder(ctx, R.style.TransparentDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialogView.findViewById<TextView>(R.id.tv_stage_reward_title).text =
                ctx.getString(R.string.home_stage_reward_dialog_title, reward.stageNumber)
            dialogView.findViewById<TextView>(R.id.tv_stage_reward_theme).text =
                reward.stageThemeTitle

            val promiseCard = dialogView.findViewById<View>(R.id.card_parent_promise)
            val promiseText = dialogView.findViewById<TextView>(R.id.tv_parent_promise)
            if (reward.hasParentPromise) {
                promiseCard.isVisible = true
                promiseText.text = reward.parentPromise
            } else {
                promiseCard.isVisible = false
            }

            val linesContainer = dialogView.findViewById<LinearLayout>(R.id.ll_stage_default_rewards)
            linesContainer.removeAllViews()
            val inflater = LayoutInflater.from(ctx)
            buildDefaultLines(ctx, reward).forEach { line ->
                val row = inflater.inflate(R.layout.item_quest_reward_celebration_line, linesContainer, false)
                row.findViewById<ImageView>(R.id.iv_reward_icon).setImageResource(line.iconRes)
                row.findViewById<TextView>(R.id.tv_reward_amount).apply {
                    text = line.amountText
                    setTextColor(ctx.getColor(R.color.child_nav_item_selected))
                }
                row.findViewById<TextView>(R.id.tv_reward_label).text = line.labelText
                linesContainer.addView(row)
            }

            val rewardsCard = dialogView.findViewById<View>(R.id.card_stage_rewards)
            rewardsCard.scaleX = 0.9f
            rewardsCard.scaleY = 0.9f
            rewardsCard.alpha = 0f
            rewardsCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(360L)
                .setInterpolator(OvershootInterpolator(1.05f))
                .start()

            dialogView.findViewById<View>(R.id.btn_stage_reward_close)
                .setOnClickListener { dialog.dismiss() }

            dialog.show()
            CelebrationDialogWindow.apply(dialog, ctx)
        } catch (_: Exception) {
            Toast.makeText(
                ctx,
                ctx.getString(R.string.home_stage_reward_toast_fallback, reward.stageNumber),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private data class RewardLine(
        val iconRes: Int,
        val amountText: String,
        val labelText: String,
    )

    private fun buildDefaultLines(ctx: android.content.Context, reward: StageRewardUi): List<RewardLine> {
        val lines = mutableListOf<RewardLine>()
        if (reward.defaultGear > 0) {
            lines += RewardLine(
                iconRes = R.drawable.ic_chip_gear,
                amountText = ctx.getString(R.string.home_stage_reward_gear_fmt, reward.defaultGear),
                labelText = ctx.getString(R.string.home_stage_reward_gear_label),
            )
        }
        if (reward.normalTickets > 0) {
            lines += RewardLine(
                iconRes = R.drawable.ic_chip_ticket,
                amountText = ctx.getString(R.string.home_stage_reward_ticket_fmt, reward.normalTickets),
                labelText = ctx.getString(R.string.home_stage_reward_ticket_label),
            )
        }
        if (lines.isEmpty()) {
            lines += RewardLine(
                iconRes = R.drawable.ic_chip_gear,
                amountText = ctx.getString(R.string.home_stage_reward_gear_default),
                labelText = ctx.getString(R.string.home_stage_reward_gear_label),
            )
        }
        return lines
    }
}
