package com.kduniv.aimong.feature.home.presentation.quest

import android.content.Context
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.quest.data.model.QuestRewardItemDto
import com.kduniv.aimong.feature.quest.domain.QuestRewardFormatter

object QuestRewardCelebrationMapper {

    fun from(
        context: Context,
        questTitle: String,
        rewards: List<QuestRewardItemDto>,
    ): QuestRewardCelebrationUi {
        val lines = rewards.mapNotNull { mapLine(context, it) }
            .ifEmpty {
                listOf(
                    QuestRewardCelebrationLine(
                        iconRes = R.drawable.ic_check_circle,
                        amountText = "+1",
                        labelText = context.getString(R.string.quest_reward_generic),
                    ),
                )
            }
        return QuestRewardCelebrationUi(
            questTitle = questTitle,
            lines = lines,
        )
    }

    private fun mapLine(context: Context, reward: QuestRewardItemDto): QuestRewardCelebrationLine? {
        return when {
            reward.isGachaTicketReward() -> {
                val count = reward.gachaTicketCount()
                if (count <= 0) return null
                QuestRewardCelebrationLine(
                    iconRes = R.drawable.ic_chip_ticket,
                    amountText = "×$count",
                    labelText = context.getString(R.string.quest_reward_line_ticket),
                )
            }
            reward.type.equals("EXP", ignoreCase = true) ||
                reward.type.equals("XP", ignoreCase = true) -> {
                if (reward.count <= 0) return null
                QuestRewardCelebrationLine(
                    iconRes = R.drawable.ic_chip_lightning,
                    amountText = "+${reward.count}",
                    labelText = context.getString(R.string.quest_reward_line_exp),
                )
            }
            reward.type.equals("GEAR", ignoreCase = true) -> {
                if (reward.count <= 0) return null
                QuestRewardCelebrationLine(
                    iconRes = R.drawable.ic_chip_gear,
                    amountText = "×${reward.count}",
                    labelText = context.getString(R.string.quest_reward_line_gear),
                )
            }
            reward.count > 0 -> QuestRewardCelebrationLine(
                iconRes = R.drawable.ic_star_filled,
                amountText = "×${reward.count}",
                labelText = reward.type,
            )
            else -> null
        }
    }

    fun fallbackToastMessage(context: Context, rewards: List<QuestRewardItemDto>): String =
        QuestRewardFormatter.format(context, rewards)
}
