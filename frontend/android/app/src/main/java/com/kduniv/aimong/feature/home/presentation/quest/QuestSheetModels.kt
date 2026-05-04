package com.kduniv.aimong.feature.home.presentation.quest

enum class QuestSheetPeriod {
    DAILY,
    WEEKLY
}

enum class QuestSheetPrimaryAction {
    COMPLETED,
    CLAIM,
    GO_LEARN,
    GO_CHAT,
    IN_PROGRESS
}

data class QuestSheetRow(
    val questType: String,
    val title: String,
    val detailText: String,
    val period: QuestSheetPeriod,
    val primaryAction: QuestSheetPrimaryAction,
    val actionEnabled: Boolean
)
