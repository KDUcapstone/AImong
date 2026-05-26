package com.kduniv.aimong.feature.dev.mock.repository

import com.kduniv.aimong.feature.gacha.data.GachaRepository
import com.kduniv.aimong.feature.gacha.data.model.FragmentGradeRow
import com.kduniv.aimong.feature.gacha.data.model.GachaExchangeData
import com.kduniv.aimong.feature.gacha.data.model.GachaFragmentsData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullResultDto
import com.kduniv.aimong.feature.gacha.data.model.RemainingTicketsDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GachaRepositoryStub @Inject constructor() : GachaRepository {

    override suspend fun pull(): Result<GachaPullData> {
        return Result.success(
            GachaPullData(
                result = GachaPullResultDto(
                    petId = "mock-pet-1",
                    petType = "pet_normal_002",
                    petName = "방울펭귄",
                    grade = "NORMAL",
                    isNew = false,
                    fragmentsGot = 2
                ),
                srMissCount = 8,
                srBonus = 0.0,
                levelUp = false,
                remainingTickets = RemainingTicketsDto(normal = 2)
            )
        )
    }

    override suspend fun getFragments(): Result<GachaFragmentsData> {
        return Result.success(
            GachaFragmentsData(
                totalCount = 13,
                fragments = listOf(
                    FragmentGradeRow("NORMAL", count = 0, exchangeThreshold = 10),
                    FragmentGradeRow("RARE", count = 0, exchangeThreshold = 30),
                    FragmentGradeRow("EPIC", count = 0, exchangeThreshold = 80),
                    FragmentGradeRow("LEGEND", count = 0, exchangeThreshold = 200),
                ),
            ),
        )
    }

    override suspend fun exchange(grade: String, petType: String): Result<GachaExchangeData> {
        return Result.success(
            GachaExchangeData(
                petId = "mock-pet-new",
                petType = petType.ifBlank { "SPROUT" },
                grade = grade.ifBlank { "RARE" },
                stage = "EGG"
            )
        )
    }
}
