package com.kduniv.aimong.feature.gacha

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.ItemGachaPetCardBinding

class GachaPetAdapter(
    private val onPetClick: (GachaPetCardUi) -> Unit
) : ListAdapter<GachaPetCardUi, GachaPetAdapter.Vh>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemGachaPetCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding, onPetClick)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position))
    }

    class Vh(
        private val binding: ItemGachaPetCardBinding,
        private val onPetClick: (GachaPetCardUi) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GachaPetCardUi) {
            val ctx = binding.root.context
            val strokeColor = if (item.isLocked) {
                ContextCompat.getColor(ctx, R.color.quiz_option_default_stroke)
            } else {
                ContextCompat.getColor(ctx, GachaUiMapper.rarityStrokeColorRes(item.grade))
            }

            binding.cardPet.strokeColor = strokeColor
            binding.viewLockedOverlay.isVisible = item.isLocked
            binding.ivLocked.isVisible = item.isLocked
            val artStage = item.pet?.stage ?: "EGG"
            PetArtAssets.bindSprite(
                image = binding.ivPetSprite,
                emojiFallback = binding.tvPetEmoji,
                petType = item.catalogPetType,
                stage = artStage,
                emoji = item.emoji,
            )
            binding.ivPetSprite.alpha = if (item.isLocked) 0.35f else 1f
            binding.tvPetEmoji.alpha = if (item.isLocked) 0.35f else 1f
            binding.tvPetName.text = item.displayName
            binding.tvPetName.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (item.isLocked) R.color.quiz_text_secondary else R.color.quiz_text_primary
                )
            )
            binding.tvPetLevel.text = item.levelLabel
            binding.tvPetLevel.isVisible = item.levelLabel.isNotBlank()

            val threshold = item.fragmentThreshold.coerceAtLeast(1)
            val count = item.fragmentCount.coerceAtLeast(0)
            val progress = ((count.toFloat() / threshold) * 100f).toInt().coerceIn(0, 100)
            binding.pbFragments.progress = progress
            binding.pbFragments.alpha = if (item.isLocked) 0.55f else 1f
            binding.tvFragmentCount.text =
                ctx.getString(R.string.gacha_fragment_progress_fmt, count, threshold)

            binding.root.setOnClickListener { onPetClick(item) }
            binding.root.isClickable = true
        }
    }

    private object Diff : DiffUtil.ItemCallback<GachaPetCardUi>() {
        override fun areItemsTheSame(a: GachaPetCardUi, b: GachaPetCardUi): Boolean =
            a.catalogPetType == b.catalogPetType

        override fun areContentsTheSame(a: GachaPetCardUi, b: GachaPetCardUi): Boolean = a == b
    }
}
