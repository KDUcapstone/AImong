package com.kduniv.aimong.feature.streak.data

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.runApi
import com.kduniv.aimong.feature.streak.data.model.StreakPartnerDto
import com.kduniv.aimong.feature.streak.data.model.StreakStatusData
import javax.inject.Inject

class StreakRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : StreakRepository {

    override suspend fun getStreak(): Result<StreakStatusData> {
        if (UiMode.useStubNav) {
            return Result.success(
                StreakStatusData(
                    continuousDays = 5,
                    lastCompletedDate = "2026-03-28",
                    todayMissionCount = 1,
                    shieldCount = 2,
                    partner = StreakPartnerDto(
                        childId = "stub-partner",
                        nickname = "지우",
                        todayCompleted = true
                    )
                )
            )
        }
        return runApi { apiService.getStreak() }
    }
}

