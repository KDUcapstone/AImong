package com.kduniv.aimong.feature.quest.domain

/**
 * 퀘스트 API v2.0+ — `MISSION_*` 진행은 일반 모드 통과한 `setId` 수 기준.
 * 보상 티켓은 NORMAL(기본 뽑기 티켓) 단일 종류만 사용한다.
 */
object QuestPolicy {

    /** AUTO: 별도 [POST /quests/claim] 호출 불가 */
    private val AUTO_CLAIM_QUEST_TYPES = setOf("MISSION_1", "CHAT_GPT")

    fun isAutoClaimQuest(questType: String): Boolean =
        questType.uppercase() in AUTO_CLAIM_QUEST_TYPES

    fun periodToApiValue(period: String): String = period.lowercase()
}
