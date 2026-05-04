package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.databinding.ItemHomeQuestBinding

class QuestListAdapter(
    private val quests: List<QuestItemUiState>
) : RecyclerView.Adapter<QuestListAdapter.QuestViewHolder>() {

    class QuestViewHolder(val binding: ItemHomeQuestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val binding = ItemHomeQuestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        val quest = quests[position]
        with(holder.binding) {
            tvQuestTitle.text = quest.title
            tvQuestReward.text = quest.rewardSummary
            tvQuestEmoji.text = "🏆"
            if (quest.isCompleted) {
                btnActionContainer.alpha = 1.0f
                tvStartBtn.text = "완료"
                tvStartBtn.setBackgroundResource(com.kduniv.aimong.R.drawable.bg_btn_completed)
                tvStartBtn.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, com.kduniv.aimong.R.color.text_grey))
            } else {
                btnActionContainer.alpha = 1.0f
                tvStartBtn.text = "시작"
                tvStartBtn.setBackgroundResource(com.kduniv.aimong.R.drawable.bg_btn_primary)
                tvStartBtn.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, com.kduniv.aimong.R.color.text_white))
            }
        }
    }

    override fun getItemCount(): Int = quests.size
}