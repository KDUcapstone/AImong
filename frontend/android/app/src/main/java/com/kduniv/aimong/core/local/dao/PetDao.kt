package com.kduniv.aimong.core.local.dao

import androidx.room.*
import com.kduniv.aimong.core.local.entity.PetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets WHERE isEquipped = 1 LIMIT 1")
    fun getEquippedPet(): Flow<PetEntity?>

    @Query("SELECT * FROM pets")
    fun getAllPets(): Flow<List<PetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity)

    @Query("UPDATE pets SET mood = :mood WHERE id = :petId")
    suspend fun updatePetMood(petId: String, mood: String)

    @Query("DELETE FROM pets")
    suspend fun clear()
}
