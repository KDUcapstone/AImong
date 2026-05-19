package com.kduniv.aimong.feature.home.presentation.quest

import android.content.Context
import com.kduniv.aimong.feature.quest.data.model.QuestRewardItemDto
import com.kduniv.aimong.feature.quest.domain.QuestRewardFormatter

/** @see QuestRewardFormatter */
object QuestRewardToastFormatter {

    fun format(context: Context, rewards: List<QuestRewardItemDto>): String =
        QuestRewardFormatter.format(context, rewards)
}
