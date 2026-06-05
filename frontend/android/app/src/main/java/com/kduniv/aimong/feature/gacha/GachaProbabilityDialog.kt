package com.kduniv.aimong.feature.gacha

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.DialogGachaProbabilitiesBinding

object GachaProbabilityDialog {

    fun show(fragment: Fragment, currentLevel: Int) {
        val context = fragment.requireContext()
        val binding = DialogGachaProbabilitiesBinding.inflate(LayoutInflater.from(context))
        var displayedLevel = currentLevel.coerceIn(
            GachaProbabilityTable.MIN_LEVEL,
            GachaProbabilityTable.MAX_LEVEL,
        )

        val dialog = Dialog(context, R.style.TransparentDialog).apply {
            setContentView(binding.root)
            setCancelable(true)
            window?.apply {
                setLayout(
                    (context.resources.displayMetrics.widthPixels * 0.9f).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0.45f)
            }
        }

        val adapter = GachaProbabilityRowAdapter()
        binding.rvProbabilities.layoutManager = LinearLayoutManager(context)
        binding.rvProbabilities.adapter = adapter

        fun bindLevel(level: Int) {
            displayedLevel = level
            val data = GachaProbabilityTable.levelData(level)
            binding.tvLevelBadge.text = context.getString(R.string.gacha_prob_level_fmt, level)
            val rangeText = context.getString(R.string.gacha_prob_pull_range_fmt, data.pullRangeLabel)
            binding.tvPullRange.text = if (level == currentLevel) {
                context.getString(R.string.gacha_prob_pull_range_current_fmt, rangeText)
            } else {
                rangeText
            }

            binding.btnLevelPrev.isEnabled = level > GachaProbabilityTable.MIN_LEVEL
            binding.btnLevelNext.isEnabled = level < GachaProbabilityTable.MAX_LEVEL
            binding.btnLevelPrev.alpha = if (binding.btnLevelPrev.isEnabled) 1f else 0.35f
            binding.btnLevelNext.alpha = if (binding.btnLevelNext.isEnabled) 1f else 0.35f

            adapter.submitList(
                data.tiers.map { tier ->
                    GachaProbabilityRowUi(
                        grade = tier.grade,
                        label = tier.label,
                        percentText = GachaProbabilityTable.formatPercent(tier.percent),
                        rarityBackgroundRes = rarityBackgroundFor(tier.grade),
                        trend = trendFor(level, tier.grade),
                    )
                },
            )
        }

        binding.btnLevelPrev.setOnClickListener {
            if (displayedLevel > GachaProbabilityTable.MIN_LEVEL) {
                bindLevel(displayedLevel - 1)
            }
        }
        binding.btnLevelNext.setOnClickListener {
            if (displayedLevel < GachaProbabilityTable.MAX_LEVEL) {
                bindLevel(displayedLevel + 1)
            }
        }
        binding.btnClose.setOnClickListener { dialog.dismiss() }

        bindLevel(displayedLevel)
        dialog.show()
    }

    private fun rarityBackgroundFor(grade: String): Int = when (grade) {
        "RARE" -> R.drawable.bg_gacha_prob_rarity_rare
        "EPIC" -> R.drawable.bg_gacha_prob_rarity_epic
        "LEGEND" -> R.drawable.bg_gacha_prob_rarity_legend
        else -> R.drawable.bg_gacha_prob_rarity_normal
    }

    private fun trendFor(level: Int, grade: String): GachaProbabilityTrend {
        val delta = GachaProbabilityTable.trendDelta(level, grade) ?: return GachaProbabilityTrend.NONE
        return when {
            delta > 0 -> GachaProbabilityTrend.UP
            delta < 0 -> GachaProbabilityTrend.DOWN
            else -> GachaProbabilityTrend.NONE
        }
    }
}
