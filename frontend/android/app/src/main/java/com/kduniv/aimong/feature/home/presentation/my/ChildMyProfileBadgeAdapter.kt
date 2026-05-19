package com.kduniv.aimong.feature.home.presentation.my

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.ItemChildMyProfileBadgeBinding

class ChildMyProfileBadgeAdapter :
    ListAdapter<ChildMyBadgeUi, ChildMyProfileBadgeAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChildMyProfileBadgeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemChildMyProfileBadgeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChildMyBadgeUi) {
            val ctx = binding.root.context
            binding.ivBadgeIcon.setImageResource(item.iconRes)
            binding.tvBadgeLabel.text = item.label
            val bgRes = if (item.isUnlocked) {
                R.drawable.bg_child_my_badge_unlocked
            } else {
                R.drawable.bg_child_my_badge_locked
            }
            binding.frameBadge.setBackgroundResource(bgRes)
            val labelColor = if (item.isUnlocked) {
                R.color.child_quest_sheet_text_primary
            } else {
                R.color.child_quest_sheet_text_secondary
            }
            binding.tvBadgeLabel.setTextColor(ContextCompat.getColor(ctx, labelColor))
            binding.ivBadgeIcon.alpha = if (item.isUnlocked) 1f else 0.45f
        }
    }

    private object Diff : DiffUtil.ItemCallback<ChildMyBadgeUi>() {
        override fun areItemsTheSame(oldItem: ChildMyBadgeUi, newItem: ChildMyBadgeUi): Boolean =
            oldItem.achievementType == newItem.achievementType &&
                oldItem.label == newItem.label

        override fun areContentsTheSame(oldItem: ChildMyBadgeUi, newItem: ChildMyBadgeUi): Boolean =
            oldItem == newItem
    }
}
