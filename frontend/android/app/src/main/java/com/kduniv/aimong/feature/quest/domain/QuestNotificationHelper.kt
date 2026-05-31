package com.kduniv.aimong.feature.quest.domain

import com.kduniv.aimong.feature.quest.data.model.ChildCustomQuestDto
import com.kduniv.aimong.feature.quest.data.model.QuestApiItemDto

/** 일일·주간 퀘스트 시트·홈 FAB 알림 집계 */
object QuestNotificationHelper {

    /** 보상이 완료됐지만 아직 수동 완료/수령하지 않은 문항 수 */
    fun countClaimable(quests: List<QuestApiItemDto>): Int =
        quests.count { dto ->
            dto.completed &&
                !dto.rewardClaimed
        }

    /**
     * 부모 실세계 미션 — 자녀에게 보여줄 알림 수.
     * - ACTIVE: 부모가 새로 만든 미션
     * - PENDING_CONFIRM: 자녀가 완료 요청 후 부모 승인 대기
     */
    fun countParentCustomQuestNotifications(quests: List<ChildCustomQuestDto>): Int =
        quests.count { quest ->
            quest.status.equals("ACTIVE", ignoreCase = true) ||
                quest.status.equals("PENDING_CONFIRM", ignoreCase = true)
        }
}
