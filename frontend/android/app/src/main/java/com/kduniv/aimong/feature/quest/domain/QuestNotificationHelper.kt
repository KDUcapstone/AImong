package com.kduniv.aimong.feature.quest.domain

import com.kduniv.aimong.feature.quest.data.model.QuestApiItemDto

/** 일일·주간 퀘스트 시트·홈 FAB 알림 집계 */
object QuestNotificationHelper {

    /** 보상이 완료됐지만 아직 수동 완료/수령하지 않은 문항 수 */
    fun countClaimable(quests: List<QuestApiItemDto>): Int =
        quests.count { dto ->
            dto.completed &&
                !dto.rewardClaimed
        }
}
