package com.kduniv.aimong.core.di

import com.kduniv.aimong.feature.auth.data.AuthRepositoryImpl
import com.kduniv.aimong.feature.auth.data.AuthRepository
import com.kduniv.aimong.feature.home.data.HomeRepositoryImpl
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import com.kduniv.aimong.feature.mission.data.MissionRepositoryImpl
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.feature.parent.data.ParentRepositoryImpl
import com.kduniv.aimong.feature.quest.data.QuestRepositoryImpl
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
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
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        homeRepositoryImpl: HomeRepositoryImpl
    ): HomeRepository

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

    @Binds
    @Singleton
    abstract fun bindParentRepository(
        parentRepositoryImpl: ParentRepositoryImpl
    ): ParentRepository

    @Binds
    @Singleton
    abstract fun bindQuestRepository(
        questRepositoryImpl: QuestRepositoryImpl
    ): QuestRepository
}
