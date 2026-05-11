package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.runApi
import com.kduniv.aimong.feature.dev.mock.StubPetGachaStore
import com.kduniv.aimong.feature.pet.data.model.PetEquipData
import com.kduniv.aimong.feature.pet.data.model.PetEquipRequest
import com.kduniv.aimong.feature.pet.data.model.PetListData
import javax.inject.Inject

class PetRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : PetRepository {

    override suspend fun getPets(): Result<PetListData> {
        if (UiMode.useStubNav) {
            return Result.success(StubPetGachaStore.getPetList())
        }
        return runApi { apiService.getPets() }
    }

    override suspend fun equipPet(petId: String): Result<PetEquipData> {
        if (UiMode.useStubNav) {
            return StubPetGachaStore.equipPet(petId)
        }
        return runApi { apiService.equipPet(PetEquipRequest(petId)) }
    }
}
