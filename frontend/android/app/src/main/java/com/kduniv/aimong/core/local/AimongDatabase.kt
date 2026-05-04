package com.kduniv.aimong.core.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kduniv.aimong.core.local.dao.ChildDao
import com.kduniv.aimong.core.local.dao.PetDao
import com.kduniv.aimong.core.local.dao.OfflineMissionQueueDao
import com.kduniv.aimong.core.local.dao.MissionDao
import com.kduniv.aimong.core.local.entity.ChildProfileEntity
import com.kduniv.aimong.core.local.entity.PetEntity
import com.kduniv.aimong.core.local.entity.OfflineMissionQueueEntity
import com.kduniv.aimong.core.local.dao.QuizDao
import com.kduniv.aimong.core.local.entity.QuizMetadataEntity
import com.kduniv.aimong.core.local.entity.QuizQuestionEntity
import com.kduniv.aimong.core.local.entity.MissionEntity

@Database(
    entities = [
        ChildProfileEntity::class,
        PetEntity::class,
        OfflineMissionQueueEntity::class,
        MissionEntity::class,
        QuizMetadataEntity::class,
        QuizQuestionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AimongDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun petDao(): PetDao
    abstract fun offlineMissionQueueDao(): OfflineMissionQueueDao
    abstract fun missionDao(): MissionDao
    abstract fun quizDao(): QuizDao
}
