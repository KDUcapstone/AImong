package com.kduniv.aimong.feature.home.presentation.quest

import com.kduniv.aimong.feature.quest.data.model.QuestApiItemDto

object QuestSheetMapper {

    fun mapItem(
        dto: QuestApiItemDto,
        period: QuestSheetPeriod,
        canStartMission: Boolean
    ): QuestSheetRow {
        val claimType = dto.claimType.uppercase()
        val lineComplete = when (claimType) {
            "MANUAL" -> dto.completed && dto.rewardClaimed
            else -> dto.completed
        }
        val progressStr = "${dto.progress.current} / ${dto.progress.required}"
        val detailText = buildString {
            append(progressStr)
            if (dto.reward.isNotBlank()) {
                append('\n')
                append(dto.reward)
            }
        }
        val missionLike = dto.questType.contains("MISSION", ignoreCase = true)
        val chatLike = dto.questType.contains("CHAT", ignoreCase = true)

        val action: Pair<QuestSheetPrimaryAction, Boolean> = when {
            lineComplete -> QuestSheetPrimaryAction.COMPLETED to false
            claimType == "MANUAL" && dto.completed && !dto.rewardClaimed ->
                QuestSheetPrimaryAction.CLAIM to true
            !dto.completed && missionLike ->
                if (canStartMission) QuestSheetPrimaryAction.GO_LEARN to true
                else QuestSheetPrimaryAction.IN_PROGRESS to false
            !dto.completed && chatLike -> QuestSheetPrimaryAction.GO_CHAT to true
            else -> QuestSheetPrimaryAction.IN_PROGRESS to false
        }

        return QuestSheetRow(
            questType = dto.questType,
            title = dto.label,
            detailText = detailText,
            period = period,
            primaryAction = action.first,
            actionEnabled = action.second
        )
    }
}
