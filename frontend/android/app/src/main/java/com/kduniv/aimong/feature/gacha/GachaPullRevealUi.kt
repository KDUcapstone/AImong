package com.kduniv.aimong.feature.gacha

/** 뽑기 결과 전체 화면 연출용 */
data class GachaPullRevealUi(
    val displayName: String,
    val petType: String,
    val grade: String,
    val emoji: String,
    val isNew: Boolean,
    val fragmentsGot: Int,
    val levelUp: Boolean,
    val remainingTickets: Int,
)
