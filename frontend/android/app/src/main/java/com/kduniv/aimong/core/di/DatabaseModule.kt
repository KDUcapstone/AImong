package com.kduniv.aimong.core.di

import android.content.Context
import androidx.room.Room
import com.kduniv.aimong.core.local.AimongDatabase
import com.kduniv.aimong.core.local.dao.MissionDao
import com.kduniv.aimong.core.local.dao.OfflineMissionQueueDao
import com.kduniv.aimong.core.local.dao.QuizDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AimongDatabase {
        return Room.databaseBuilder(
            context,
            AimongDatabase::class.java,
            "aimong.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMissionDao(database: AimongDatabase): MissionDao = database.missionDao()

    @Provides
    fun provideOfflineMissionQueueDao(database: AimongDatabase): OfflineMissionQueueDao = database.offlineMissionQueueDao()

    @Provides
    fun provideQuizDao(database: AimongDatabase): QuizDao = database.quizDao()
}
