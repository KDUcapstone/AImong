package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.core.local.entity.PetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class PetRepositoryImpl @Inject constructor() : PetRepository {
    override fun getEquippedPet(): Flow<PetEntity?> {
        // TODO: Room DB 연동
        return flowOf(null)
    }

    override fun getAllPets(): Flow<List<PetEntity>> {
        return flowOf(emptyList())
    }

    override suspend fun updatePetMood(petId: String, mood: String) {
        // TODO: Update logic
    }

    override suspend fun syncPetsFromServer() {
        // TODO: Sync logic
    }
}
