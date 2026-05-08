package com.kduniv.aimong.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kduniv.aimong.core.local.entity.MissionSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionSetDao {
    @Query("SELECT * FROM mission_sets ORDER BY levelNo ASC, stage ASC, setId ASC")
    fun getMissionSets(): Flow<List<MissionSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissionSets(items: List<MissionSetEntity>)

    @Query("DELETE FROM mission_sets")
    suspend fun clearMissionSets()
}

