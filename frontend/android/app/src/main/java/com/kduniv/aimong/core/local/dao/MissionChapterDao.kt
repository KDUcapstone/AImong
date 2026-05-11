package com.kduniv.aimong.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kduniv.aimong.core.local.entity.MissionChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionChapterDao {
    @Query("SELECT * FROM mission_chapters ORDER BY stage ASC, missionId ASC")
    fun getChapters(): Flow<List<MissionChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(items: List<MissionChapterEntity>)

    @Query("DELETE FROM mission_chapters")
    suspend fun clearChapters()
}
