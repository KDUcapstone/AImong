package com.kduniv.aimong.feature.gacha

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.ItemGachaProbabilityRowBinding

data class GachaProbabilityRowUi(
    val grade: String,
    val label: String,
    val percentText: String,
  @DrawableRes val rarityBackgroundRes: Int,
    val trend: GachaProbabilityTrend,
)

enum class GachaProbabilityTrend {
    NONE,
    UP,
    DOWN,
}

class GachaProbabilityRowAdapter :
    ListAdapter<GachaProbabilityRowUi, GachaProbabilityRowAdapter.Vh>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemGachaProbabilityRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Vh(binding)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position))
    }

    class Vh(
        private val binding: ItemGachaProbabilityRowBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GachaProbabilityRowUi) {
            binding.tvRarityLabel.text = item.label
            binding.tvRarityLabel.setBackgroundResource(item.rarityBackgroundRes)
            binding.tvPercent.text = item.percentText
            when (item.trend) {
                GachaProbabilityTrend.UP -> {
                    binding.ivTrend.isVisible = true
                    binding.ivTrend.setImageResource(R.drawable.ic_gacha_prob_trend_up)
                }
                GachaProbabilityTrend.DOWN -> {
                    binding.ivTrend.isVisible = true
                    binding.ivTrend.setImageResource(R.drawable.ic_gacha_prob_trend_down)
                }
                GachaProbabilityTrend.NONE -> binding.ivTrend.isVisible = false
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<GachaProbabilityRowUi>() {
        override fun areItemsTheSame(
            oldItem: GachaProbabilityRowUi,
            newItem: GachaProbabilityRowUi,
        ): Boolean = oldItem.grade == newItem.grade

        override fun areContentsTheSame(
            oldItem: GachaProbabilityRowUi,
            newItem: GachaProbabilityRowUi,
        ): Boolean = oldItem == newItem
    }
}
