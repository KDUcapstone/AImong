package com.kduniv.aimong.feature.gacha

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.ItemGachaPetCardBinding
import com.kduniv.aimong.feature.pet.domain.PetGrowthRules

class GachaPetAdapter(
    private val onPetClick: (GachaPetCardUi) -> Unit
) : ListAdapter<GachaPetCardUi, GachaPetAdapter.Vh>(Diff) {

    companion object {
        private val LOCKED_SPRITE_FILTER =
            ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemGachaPetCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding, onPetClick)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: Vh) {
        holder.clearSpriteRequest()
        super.onViewRecycled(holder)
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
            binding.tvBadgeEquipped.isVisible = item.isEquipped && !item.isLocked
            binding.ivLocked.isVisible = item.isLocked
            val artStage = when {
                item.isLocked -> "GROWTH"
                item.pet != null -> PetGrowthRules.resolveEffectiveStageString(
                    item.pet.stage,
                    item.pet.xp,
                )
                else -> "EGG"
            }
            val allowStageFallback = item.isLocked
            PetArtAssets.bindSprite(
                image = binding.ivPetSprite,
                emojiFallback = binding.tvPetEmoji,
                petType = item.catalogPetType,
                stage = artStage,
                emoji = item.emoji,
                allowStageFallback = allowStageFallback,
            )
            if (item.isLocked) {
                binding.ivPetSprite.colorFilter = GachaPetAdapter.LOCKED_SPRITE_FILTER
                binding.ivPetSprite.alpha = 0.5f
                binding.tvPetEmoji.alpha = 0.5f
            } else {
                binding.ivPetSprite.colorFilter = null
                binding.ivPetSprite.alpha = 1f
                binding.tvPetEmoji.alpha = 1f
            }
            binding.tvPetName.text = item.displayName
            binding.tvPetName.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (item.isLocked) R.color.quiz_text_secondary else R.color.quiz_text_primary
                )
            )
            if (item.isLocked) {
                binding.pbFragments.isVisible = true
                binding.tvFragmentCount.isVisible = true
                binding.pbPetXp.isVisible = false
                binding.tvPetXpCount.isVisible = false
                val threshold = item.fragmentThreshold.coerceAtLeast(1)
                val count = item.fragmentCount.coerceAtLeast(0)
                val progress = ((count.toFloat() / threshold) * 100f).toInt().coerceIn(0, 100)
                binding.pbFragments.progress = progress
                binding.pbFragments.alpha = 0.55f
                binding.tvFragmentCount.text =
                    ctx.getString(R.string.gacha_fragment_progress_fmt, count, threshold)
            } else {
                binding.pbFragments.isVisible = false
                binding.tvFragmentCount.isVisible = false
                binding.pbPetXp.isVisible = true
                binding.tvPetXpCount.isVisible = true
                item.pet?.let { pet ->
                    GachaUiMapper.bindPetCardXp(
                        binding.pbPetXp,
                        binding.tvPetXpCount,
                        ctx,
                        pet,
                    )
                }
            }

            binding.root.setOnClickListener { onPetClick(item) }
            binding.root.isClickable = true
        }

        fun clearSpriteRequest() {
            PetArtAssets.clearSprite(binding.ivPetSprite)
        }
    }

    private object Diff : DiffUtil.ItemCallback<GachaPetCardUi>() {
        override fun areItemsTheSame(a: GachaPetCardUi, b: GachaPetCardUi): Boolean =
            a.catalogPetType == b.catalogPetType

        override fun areContentsTheSame(a: GachaPetCardUi, b: GachaPetCardUi): Boolean = a == b
    }
}
