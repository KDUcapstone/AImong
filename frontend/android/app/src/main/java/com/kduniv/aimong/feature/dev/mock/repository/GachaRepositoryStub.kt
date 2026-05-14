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

    override suspend fun pull(ticketType: String): Result<GachaPullData> {
        return Result.success(
            GachaPullData(
                result = GachaPullResultDto(
                    petId = "mock-pet-1",
                    petType = "SPROUT",
                    petName = "목업 펫",
                    grade = "NORMAL",
                    isNew = false,
                    fragmentsGot = 2
                ),
                srMissCount = 0,
                srBonus = 0.0,
                levelUp = false,
                remainingTickets = RemainingTicketsDto(normal = 10, rare = 2, epic = 1)
            )
        )
    }

    override suspend fun getFragments(): Result<GachaFragmentsData> {
        return Result.success(
            GachaFragmentsData(
                fragments = listOf(
                    FragmentGradeRow("NORMAL", count = 12, exchangeThreshold = 20),
                    FragmentGradeRow("RARE", count = 4, exchangeThreshold = 10),
                    FragmentGradeRow("EPIC", count = 1, exchangeThreshold = 5)
                )
            )
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
