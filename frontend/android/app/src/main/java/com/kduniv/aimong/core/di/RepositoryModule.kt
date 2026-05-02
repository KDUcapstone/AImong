package com.kduniv.aimong.core.di

import com.kduniv.aimong.feature.mission.data.MissionRepositoryImpl
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import com.kduniv.aimong.feature.quiz.data.QuizRepositoryImpl
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMissionRepository(
        missionRepositoryImpl: MissionRepositoryImpl
    ): MissionRepository

    @Binds
    @Singleton
    abstract fun bindQuizRepository(
        quizRepositoryImpl: QuizRepositoryImpl
    ): QuizRepository
}
