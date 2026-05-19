package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.feature.home.data.model.TicketsDto
import com.kduniv.aimong.feature.home.data.model.TopStatusDto

/** GET /home 티켓 — `topStatus.ticketCount` 와 `tickets.normal` 정합 (v2.3 단일 티켓) */
object TicketTotals {

    fun sum(tickets: TicketsDto): Int = tickets.normal

    /** 상단 뽑기 티켓 칩 — 가챠·POST /quests/claim 과 동일하게 `tickets.normal` 기준 */
    fun displayTotal(top: TopStatusDto, tickets: TicketsDto): Int = when {
        tickets.normal > 0 -> tickets.normal
        top.ticketCount > 0 -> top.ticketCount
        else -> 0
    }
}
