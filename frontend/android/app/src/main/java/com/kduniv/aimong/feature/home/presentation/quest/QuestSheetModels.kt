package com.kduniv.aimong.feature.home.presentation.quest

enum class QuestSheetPeriod {
    DAILY,
    WEEKLY,
    PARENT,
}

enum class QuestSheetPrimaryAction {
    COMPLETED,
    CLAIM,
    GO_LEARN,
    GO_CHAT,
    IN_PROGRESS,
    /** 부모 커스텀 퀘스트 — ACTIVE */
    COMPLETE_CUSTOM,
    /** 부모 커스텀 퀘스트 — PENDING_CONFIRM */
    AWAITING_CONFIRM,
}

data class QuestSheetRow(
    val questType: String,
    val title: String,
    val detailText: String,
    val period: QuestSheetPeriod,
    val primaryAction: QuestSheetPrimaryAction,
    val actionEnabled: Boolean,
    val isCustomQuest: Boolean = false,
)
