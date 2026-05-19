package com.kduniv.aimong.feature.gacha

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.databinding.ItemGachaOwnedPetBinding

class GachaOwnedPetAdapter(
    private val onPetClick: (GachaPetCardUi) -> Unit
) : ListAdapter<GachaPetCardUi, GachaOwnedPetAdapter.Vh>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemGachaOwnedPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding, onPetClick)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position))
    }

    class Vh(
        private val binding: ItemGachaOwnedPetBinding,
        private val onPetClick: (GachaPetCardUi) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GachaPetCardUi) {
            val ctx = binding.root.context
            binding.root.strokeColor = ContextCompat.getColor(
                ctx,
                GachaUiMapper.rarityStrokeColorRes(item.grade)
            )
            binding.tvBadgeEquipped.isVisible = item.isEquipped
            val pet = item.pet
            PetArtAssets.bindSprite(
                image = binding.ivPetSprite,
                emojiFallback = binding.tvPetEmoji,
                petType = pet?.petType ?: item.catalogPetType,
                stage = pet?.stage,
                emoji = item.emoji,
            )
            binding.tvPetName.text = item.displayName
            binding.root.setOnClickListener { onPetClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<GachaPetCardUi>() {
        override fun areItemsTheSame(a: GachaPetCardUi, b: GachaPetCardUi): Boolean =
            a.pet?.id == b.pet?.id

        override fun areContentsTheSame(a: GachaPetCardUi, b: GachaPetCardUi): Boolean = a == b
    }
}
