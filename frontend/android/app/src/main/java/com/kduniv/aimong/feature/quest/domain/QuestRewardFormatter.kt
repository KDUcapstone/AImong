package com.kduniv.aimong.feature.quest.domain

import android.content.Context
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.quest.data.model.QuestRewardItemDto

/** [POST /quests/claim] 보상 목록 → 사용자 문구 (단일 기본 티켓) */
object QuestRewardFormatter {

    fun format(context: Context, rewards: List<QuestRewardItemDto>): String {
        if (rewards.isEmpty()) return context.getString(R.string.quest_reward_generic)
        return rewards.joinToString(separator = context.getString(R.string.quest_reward_separator)) { r ->
            formatOne(context, r)
        }
    }

    private fun formatOne(context: Context, r: QuestRewardItemDto): String {
        return when {
            r.isGachaTicketReward() ->
                context.getString(R.string.quest_reward_gacha_ticket, r.gachaTicketCount())
            r.type.equals("EXP", ignoreCase = true) && r.count > 0 ->
                context.getString(R.string.quest_reward_exp, r.count)
            r.type.equals("XP", ignoreCase = true) && r.count > 0 ->
                context.getString(R.string.quest_reward_exp, r.count)
            else -> context.getString(R.string.quest_reward_generic)
        }
    }
}
