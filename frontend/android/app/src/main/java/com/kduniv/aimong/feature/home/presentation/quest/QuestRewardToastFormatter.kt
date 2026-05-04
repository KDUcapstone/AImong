package com.kduniv.aimong.feature.home.presentation.quest

import android.content.Context
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.quest.data.model.QuestRewardItemDto

object QuestRewardToastFormatter {

    fun format(context: Context, rewards: List<QuestRewardItemDto>): String {
        if (rewards.isEmpty()) return context.getString(R.string.quest_reward_generic)
        return rewards.joinToString(separator = context.getString(R.string.quest_reward_separator)) { r ->
            formatOne(context, r)
        }
    }

    private fun formatOne(context: Context, r: QuestRewardItemDto): String {
        return when (r.type.uppercase()) {
            "TICKET" -> when (r.ticketType?.uppercase()) {
                "NORMAL" -> context.getString(R.string.quest_reward_ticket_normal, r.count)
                "RARE" -> context.getString(R.string.quest_reward_ticket_rare, r.count)
                "EPIC" -> context.getString(R.string.quest_reward_ticket_epic, r.count)
                else -> context.getString(R.string.quest_reward_ticket_generic, r.count)
            }
            else -> context.getString(R.string.quest_reward_generic)
        }
    }
}
