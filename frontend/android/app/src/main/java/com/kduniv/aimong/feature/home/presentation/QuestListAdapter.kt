package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.ItemHomeQuestBinding
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetRow

class QuestListAdapter(
    private val onRowInteraction: (QuestSheetRow) -> Unit
) : ListAdapter<QuestSheetRow, QuestListAdapter.QuestViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val binding = ItemHomeQuestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuestViewHolder(binding, onRowInteraction)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class QuestViewHolder(
        private val binding: ItemHomeQuestBinding,
        private val onRowInteraction: (QuestSheetRow) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: QuestSheetRow) {
            val ctx = binding.root.context
            binding.tvQuestTitle.text = row.title
            binding.tvQuestReward.text = row.detailText
            binding.tvQuestEmoji.text = "🏆"

            val label: String
            val bgRes: Int
            val textColorRes: Int
            val containerAlpha: Float
            when (row.primaryAction) {
                QuestSheetPrimaryAction.COMPLETED -> {
                    label = ctx.getString(R.string.quest_action_completed)
                    bgRes = R.drawable.bg_btn_completed
                    textColorRes = R.color.text_grey
                    containerAlpha = 1f
                }
                QuestSheetPrimaryAction.CLAIM -> {
                    label = ctx.getString(R.string.quest_action_claim)
                    bgRes = R.drawable.bg_btn_primary
                    textColorRes = R.color.text_white
                    containerAlpha = if (row.actionEnabled) 1f else 0.45f
                }
                QuestSheetPrimaryAction.GO_LEARN -> {
                    label = ctx.getString(R.string.quest_action_go_learn)
                    bgRes = R.drawable.bg_btn_primary
                    textColorRes = R.color.text_white
                    containerAlpha = if (row.actionEnabled) 1f else 0.45f
                }
                QuestSheetPrimaryAction.GO_CHAT -> {
                    label = ctx.getString(R.string.quest_action_go_chat)
                    bgRes = R.drawable.bg_btn_primary
                    textColorRes = R.color.text_white
                    containerAlpha = if (row.actionEnabled) 1f else 0.45f
                }
                QuestSheetPrimaryAction.IN_PROGRESS -> {
                    label = ctx.getString(R.string.quest_action_in_progress)
                    bgRes = R.drawable.bg_btn_completed
                    textColorRes = R.color.text_grey
                    containerAlpha = 0.75f
                }
            }

            binding.tvStartBtn.text = label
            binding.tvStartBtn.setBackgroundResource(bgRes)
            binding.tvStartBtn.setTextColor(ContextCompat.getColor(ctx, textColorRes))
            binding.btnActionContainer.alpha = containerAlpha

            binding.btnActionContainer.setOnClickListener {
                when (row.primaryAction) {
                    QuestSheetPrimaryAction.CLAIM,
                    QuestSheetPrimaryAction.GO_LEARN,
                    QuestSheetPrimaryAction.GO_CHAT -> {
                        if (row.actionEnabled) onRowInteraction(row)
                    }
                    QuestSheetPrimaryAction.COMPLETED,
                    QuestSheetPrimaryAction.IN_PROGRESS -> Unit
                }
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<QuestSheetRow>() {
        override fun areItemsTheSame(oldItem: QuestSheetRow, newItem: QuestSheetRow): Boolean =
            oldItem.questType == newItem.questType && oldItem.period == newItem.period

        override fun areContentsTheSame(oldItem: QuestSheetRow, newItem: QuestSheetRow): Boolean =
            oldItem == newItem
    }
}
