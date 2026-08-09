package com.kduniv.aimong.feature.quest.domain.repository

import com.kduniv.aimong.feature.quest.data.model.ChildCustomQuestCompleteResponseData
import com.kduniv.aimong.feature.quest.data.model.ChildCustomQuestListResponseData
import com.kduniv.aimong.feature.quest.data.model.DailyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestClaimResponseData
import com.kduniv.aimong.feature.quest.data.model.WeeklyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.AchievementsResponseData

interface QuestRepository {
    suspend fun getDailyQuests(): Result<DailyQuestsResponseData>
    suspend fun getWeeklyQuests(): Result<WeeklyQuestsResponseData>
    suspend fun claimQuest(questType: String, period: String): Result<QuestClaimResponseData>
    suspend fun getAchievements(): Result<AchievementsResponseData>
    suspend fun getChildCustomQuests(): Result<ChildCustomQuestListResponseData>
    suspend fun completeChildCustomQuest(questId: String): Result<ChildCustomQuestCompleteResponseData>
}
