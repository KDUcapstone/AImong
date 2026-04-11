package com.kduniv.aimong.core.local.dao

import androidx.room.*
import com.kduniv.aimong.core.local.entity.OfflineMissionQueueEntity

@Dao
interface OfflineMissionQueueDao {
    @Query("SELECT * FROM offline_mission_queue WHERE isSync = 0")
    suspend fun getUnsyncedMissions(): List<OfflineMissionQueueEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMission(mission: OfflineMissionQueueEntity)

    @Query("UPDATE offline_mission_queue SET isSync = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("DELETE FROM offline_mission_queue WHERE isSync = 1")
    suspend fun clearSyncedMissions()
}
