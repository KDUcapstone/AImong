package com.kduniv.aimong.feature.gacha

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.databinding.ItemGachaPetCardBinding
import com.kduniv.aimong.feature.pet.data.model.PetDto

class GachaPetAdapter(
    private val onEquip: (String) -> Unit
) : ListAdapter<PetDto, GachaPetAdapter.Vh>(Diff) {

    var equippedPetId: String? = null
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemGachaPetCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding, onEquip)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position), equippedPetId)
    }

    class Vh(
        private val binding: ItemGachaPetCardBinding,
        private val onEquip: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pet: PetDto, equippedId: String?) {
            val isEquipped = pet.id == equippedId
            binding.tvBadgeEquipped.isVisible = isEquipped
            binding.tvPetName.text = pet.petType
            binding.tvPetMeta.text = "${pet.grade} · ${pet.stage}"
            binding.tvPetXp.text = "XP ${pet.xp}"

            // 카드 탭으로 장착 (단일 장착)
            binding.root.setOnClickListener {
                if (!isEquipped) onEquip(pet.id)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<PetDto>() {
        override fun areItemsTheSame(a: PetDto, b: PetDto): Boolean = a.id == b.id
        override fun areContentsTheSame(a: PetDto, b: PetDto): Boolean = a == b
    }
}
