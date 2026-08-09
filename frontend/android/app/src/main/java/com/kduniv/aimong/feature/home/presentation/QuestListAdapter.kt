package com.kduniv.aimong.feature.home.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
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

    private var sheetLoading: Boolean = false

    /** 바텀시트에서 목록을 다시 불러오는 동안 행의 주요 버튼을 누르지 못하게 합니다. */
    fun setSheetLoading(loading: Boolean) {
        if (sheetLoading == loading) return
        sheetLoading = loading
        val n = itemCount
        if (n > 0) notifyItemRangeChanged(0, n)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val binding = ItemHomeQuestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuestViewHolder(binding, onRowInteraction)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        holder.bind(getItem(position), sheetLoading)
    }

    class QuestViewHolder(
        private val binding: ItemHomeQuestBinding,
        private val onRowInteraction: (QuestSheetRow) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: QuestSheetRow, sheetLoading: Boolean) {
            val ctx = binding.root.context
            val loading = sheetLoading
            binding.tvQuestTitle.text = row.title
            binding.tvQuestReward.text = row.detailText

            val (iconRes, iconTint) = when {
                row.isCustomQuest ->
                    R.drawable.ic_role_parent to ContextCompat.getColor(ctx, R.color.child_nav_item_selected)
                row.primaryAction == QuestSheetPrimaryAction.COMPLETED ->
                    R.drawable.ic_check_circle to ContextCompat.getColor(ctx, R.color.child_nav_item_selected)
                row.primaryAction == QuestSheetPrimaryAction.IN_PROGRESS ->
                    R.drawable.ic_play_arrow to ContextCompat.getColor(ctx, R.color.child_quest_sheet_text_secondary)
                else ->
                    R.drawable.ic_star_filled to ContextCompat.getColor(ctx, R.color.child_nav_item_selected)
            }
            binding.ivQuestRowIcon.setImageResource(iconRes)
            ImageViewCompat.setImageTintList(binding.ivQuestRowIcon, ColorStateList.valueOf(iconTint))
            binding.viewQuestRowNotification.isVisible = row.showNotificationDot

            val label: String
            val bgRes: Int
            val textColorRes: Int
            val containerAlpha: Float
            when (row.primaryAction) {
                QuestSheetPrimaryAction.COMPLETED -> {
                    label = if (row.isCustomQuest) {
                        ctx.getString(R.string.child_custom_quest_status_done)
                    } else {
                        ctx.getString(R.string.quest_action_completed)
                    }
                    bgRes = R.drawable.bg_child_quest_action_outline
                    textColorRes = R.color.child_quest_sheet_text_secondary
                    containerAlpha = 1f
                }
                QuestSheetPrimaryAction.CLAIM -> {
                    label = ctx.getString(R.string.quest_action_claim)
                    bgRes = R.drawable.bg_child_quest_action_filled
                    textColorRes = R.color.text_white
                    val enabled = row.actionEnabled && !loading
                    containerAlpha = if (enabled) 1f else 0.45f
                }
                QuestSheetPrimaryAction.GO_LEARN -> {
                    label = ctx.getString(R.string.quest_action_go_learn)
                    bgRes = R.drawable.bg_child_quest_action_filled
                    textColorRes = R.color.text_white
                    val enabled = row.actionEnabled && !loading
                    containerAlpha = if (enabled) 1f else 0.45f
                }
                QuestSheetPrimaryAction.GO_CHAT -> {
                    label = ctx.getString(R.string.quest_action_go_chat)
                    bgRes = R.drawable.bg_child_quest_action_filled
                    textColorRes = R.color.text_white
                    val enabled = row.actionEnabled && !loading
                    containerAlpha = if (enabled) 1f else 0.45f
                }
                QuestSheetPrimaryAction.IN_PROGRESS -> {
                    label = ctx.getString(R.string.quest_action_in_progress)
                    bgRes = R.drawable.bg_child_quest_action_outline
                    textColorRes = R.color.child_quest_sheet_text_secondary
                    containerAlpha = 0.85f
                }
                QuestSheetPrimaryAction.COMPLETE_CUSTOM -> {
                    label = ctx.getString(R.string.child_custom_quest_action_complete)
                    bgRes = R.drawable.bg_child_quest_action_filled
                    textColorRes = R.color.text_white
                    val enabled = row.actionEnabled && !loading
                    containerAlpha = if (enabled) 1f else 0.45f
                }
                QuestSheetPrimaryAction.AWAITING_CONFIRM -> {
                    label = ctx.getString(R.string.child_custom_quest_action_pending)
                    bgRes = R.drawable.bg_child_quest_action_outline
                    textColorRes = R.color.child_quest_sheet_text_secondary
                    containerAlpha = 0.85f
                }
            }

            binding.tvStartBtn.text = label
            binding.tvStartBtn.setBackgroundResource(bgRes)
            binding.tvStartBtn.setTextColor(ContextCompat.getColor(ctx, textColorRes))
            binding.btnActionContainer.alpha = containerAlpha

            binding.btnActionContainer.setOnClickListener {
                if (loading) return@setOnClickListener
                when (row.primaryAction) {
                    QuestSheetPrimaryAction.CLAIM,
                    QuestSheetPrimaryAction.GO_LEARN,
                    QuestSheetPrimaryAction.GO_CHAT,
                    QuestSheetPrimaryAction.COMPLETE_CUSTOM -> {
                        if (row.actionEnabled) onRowInteraction(row)
                    }
                    QuestSheetPrimaryAction.COMPLETED,
                    QuestSheetPrimaryAction.IN_PROGRESS,
                    QuestSheetPrimaryAction.AWAITING_CONFIRM -> Unit
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
