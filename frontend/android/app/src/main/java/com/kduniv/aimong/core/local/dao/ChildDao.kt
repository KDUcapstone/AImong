package com.kduniv.aimong.core.local.dao

import androidx.room.*
import com.kduniv.aimong.core.local.entity.ChildProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM child_profiles LIMIT 1")
    fun getChildProfile(): Flow<ChildProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildProfile(profile: ChildProfileEntity)

    @Query("DELETE FROM child_profiles")
    suspend fun clear()
}
