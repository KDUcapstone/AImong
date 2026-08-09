package com.kduniv.aimong.feature.gacha.data

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.runApi
import com.kduniv.aimong.feature.dev.mock.StubPetGachaStore
import com.kduniv.aimong.feature.gacha.data.model.GachaExchangeData
import com.kduniv.aimong.feature.gacha.data.model.GachaExchangeRequest
import com.kduniv.aimong.feature.gacha.data.model.GachaFragmentsData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullRequest
import com.kduniv.aimong.feature.gacha.data.model.GachaTicketType
import javax.inject.Inject

class GachaRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : GachaRepository {

    override suspend fun pull(): Result<GachaPullData> {
        if (UiMode.useStubNav) {
            return StubPetGachaStore.pull()
        }
        return runApi { apiService.gachaPull(GachaPullRequest(GachaTicketType.NORMAL)) }
    }

    override suspend fun getFragments(): Result<GachaFragmentsData> {
        if (UiMode.useStubNav) {
            return Result.success(StubPetGachaStore.getFragments())
        }
        return runApi { apiService.getGachaFragments() }
    }

    override suspend fun exchange(grade: String, petType: String): Result<GachaExchangeData> {
        if (UiMode.useStubNav) {
            return StubPetGachaStore.exchange(grade, petType)
        }
        return runApi { apiService.gachaExchange(GachaExchangeRequest(grade, petType)) }
    }
}
