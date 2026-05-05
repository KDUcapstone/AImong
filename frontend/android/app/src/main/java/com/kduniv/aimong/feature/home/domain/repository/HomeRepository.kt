package com.kduniv.aimong.feature.home.domain.repository

import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.domain.model.StreakCalendarResult

interface HomeRepository {
    suspend fun getHome(): Result<HomeScreenData>

    /**
     * GET /home/streak-calendar
     * @param yearMonth `YYYY-MM`. null이면 서버가 KST 현재 월 사용.
     */
    suspend fun getStreakCalendar(yearMonth: String? = null): Result<StreakCalendarResult>
}
