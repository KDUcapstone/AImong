package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.feature.pet.data.model.PetEquipData
import com.kduniv.aimong.feature.pet.data.model.PetListData

interface PetRepository {
    suspend fun getPets(): Result<PetListData>
    suspend fun equipPet(petId: String): Result<PetEquipData>
}
