package com.kduniv.aimong.core.local.dao

import androidx.room.*
import com.kduniv.aimong.core.local.entity.MissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY stage ASC")
    fun getMissions(): Flow<List<MissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<MissionEntity>)

    @Query("DELETE FROM missions")
    suspend fun clearMissions()
}
