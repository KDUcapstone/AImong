package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.feature.home.data.model.TicketsDto
import com.kduniv.aimong.feature.home.data.model.TopStatusDto

/** GET /home 티켓 — `topStatus.ticketCount` 와 `tickets.normal` 정합 (v2.3 단일 티켓) */
object TicketTotals {

    fun sum(tickets: TicketsDto): Int = tickets.normal

    /** 상단 뽑기 티켓 칩: topStatus.ticketCount 우선, 없으면 tickets.normal */
    fun displayTotal(top: TopStatusDto, tickets: TicketsDto): Int = when {
        top.ticketCount > 0 -> top.ticketCount
        tickets.normal > 0 -> tickets.normal
        else -> 0
    }
}
