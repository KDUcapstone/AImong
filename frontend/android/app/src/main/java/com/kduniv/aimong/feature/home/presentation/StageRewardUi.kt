package com.kduniv.aimong.feature.home.presentation

import com.kduniv.aimong.feature.home.data.model.ChildStageRewardDto

data class StageRewardUi(
    val stageNumber: Int,
    val stageThemeTitle: String,
    val parentPromise: String?,
    val defaultGear: Int,
    val normalTickets: Int,
    val isTriggered: Boolean = false,
) {
    val hasParentPromise: Boolean
        get() = !parentPromise.isNullOrBlank()

    companion object {
        private val STAGE_THEMES = mapOf(
            1 to "AI가 뭐예요?",
            2 to "AI 잘 쓰기",
            3 to "비판적으로 생각하기",
        )

        fun fromDto(dto: ChildStageRewardDto): StageRewardUi = StageRewardUi(
            stageNumber = dto.stageNumber,
            stageThemeTitle = STAGE_THEMES[dto.stageNumber] ?: "${dto.stageNumber}단계",
            parentPromise = dto.rewardText?.trim()?.takeIf { it.isNotEmpty() },
            defaultGear = dto.defaultGearReward.coerceAtLeast(0),
            normalTickets = dto.normalTicketReward.coerceAtLeast(0),
            isTriggered = dto.isTriggered,
        )

        fun defaultsForStages(stageNumbers: List<Int>): Map<Int, StageRewardUi> =
            stageNumbers.associateWith { stage ->
                fromDto(
                    ChildStageRewardDto(
                        stageNumber = stage,
                        rewardText = null,
                        defaultGearReward = defaultGearFor(stage),
                        normalTicketReward = defaultTicketsFor(stage),
                    ),
                )
            }

        private fun defaultGearFor(stage: Int): Int = when (stage) {
            2 -> 50
            3 -> 80
            else -> 30
        }

        private fun defaultTicketsFor(stage: Int): Int = when (stage) {
            3 -> 3
            else -> 0
        }
    }
}
