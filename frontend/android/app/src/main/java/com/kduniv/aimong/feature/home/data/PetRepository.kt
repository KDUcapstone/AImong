package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.core.local.entity.PetEntity
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    fun getEquippedPet(): Flow<PetEntity?>
    fun getAllPets(): Flow<List<PetEntity>>
    suspend fun updatePetMood(petId: String, mood: String)
    suspend fun syncPetsFromServer()
}
