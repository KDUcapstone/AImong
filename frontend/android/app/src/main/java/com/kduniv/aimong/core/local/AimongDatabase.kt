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
import com.kduniv.aimong.core.local.entity.MissionChapterEntity
import com.kduniv.aimong.core.local.dao.MissionChapterDao

@Database(
    entities = [
        ChildProfileEntity::class,
        PetEntity::class,
        OfflineMissionQueueEntity::class,
        MissionEntity::class,
        MissionChapterEntity::class,
        QuizMetadataEntity::class,
        QuizQuestionEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AimongDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun petDao(): PetDao
    abstract fun offlineMissionQueueDao(): OfflineMissionQueueDao
    abstract fun missionDao(): MissionDao
    abstract fun missionChapterDao(): MissionChapterDao
    abstract fun quizDao(): QuizDao
}
