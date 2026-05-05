package com.kduniv.aimong.feature.quest.domain.repository

import com.kduniv.aimong.feature.quest.data.model.DailyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestClaimResponseData
import com.kduniv.aimong.feature.quest.data.model.WeeklyQuestsResponseData

interface QuestRepository {
    suspend fun getDailyQuests(): Result<DailyQuestsResponseData>
    suspend fun getWeeklyQuests(): Result<WeeklyQuestsResponseData>
    suspend fun claimQuest(questType: String, period: String): Result<QuestClaimResponseData>
}
