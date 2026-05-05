package com.kduniv.aimong.feature.parent.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.databinding.ItemParentChildBinding

class ParentChildAdapter(
    private val onRegenerateCode: (String) -> Unit
) : ListAdapter<ParentChildItem, ParentChildAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParentChildBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemParentChildBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ParentChildItem) {
            binding.tvChildNickname.text = item.nickname
            binding.tvChildXp.text = "누적 XP: ${item.totalXp}"
            binding.tvChildCode.text = item.code

            binding.btnRegenerateCode.setOnClickListener {
                onRegenerateCode(item.childId)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ParentChildItem>() {
            override fun areItemsTheSame(oldItem: ParentChildItem, newItem: ParentChildItem): Boolean {
                return oldItem.childId == newItem.childId
            }

            override fun areContentsTheSame(oldItem: ParentChildItem, newItem: ParentChildItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
