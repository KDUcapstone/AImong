package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.feature.home.data.model.TicketsDto
import com.kduniv.aimong.feature.home.data.model.TopStatusDto

/** GET /home 티켓 — topStatus.ticketCount 와 tickets breakdown 정합 */
object TicketTotals {

    fun sum(tickets: TicketsDto): Int =
        tickets.normal + tickets.rare + tickets.epic

    /** 홈 칩·요약용: 등급별 합이 있으면 그것을, 없으면 topStatus 요약값 */
    fun displayTotal(top: TopStatusDto, tickets: TicketsDto): Int {
        val breakdownSum = sum(tickets)
        return if (breakdownSum > 0) breakdownSum else top.ticketCount
    }
}
