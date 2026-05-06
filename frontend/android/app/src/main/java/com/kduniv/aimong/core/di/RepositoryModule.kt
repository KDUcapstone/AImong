package com.kduniv.aimong.core.di

import com.kduniv.aimong.feature.auth.data.AuthRepositoryImpl
import com.kduniv.aimong.feature.auth.data.AuthRepository
import com.kduniv.aimong.feature.gacha.data.GachaRepository
import com.kduniv.aimong.feature.gacha.data.GachaRepositoryImpl
import com.kduniv.aimong.feature.home.data.HomeRepositoryImpl
import com.kduniv.aimong.feature.home.data.PetRepository
import com.kduniv.aimong.feature.home.data.PetRepositoryImpl
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import com.kduniv.aimong.feature.mission.data.MissionRepositoryImpl
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.feature.parent.data.ParentRepositoryImpl
import com.kduniv.aimong.feature.quest.data.QuestRepositoryImpl
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import com.kduniv.aimong.feature.quiz.data.QuizRepositoryImpl
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import com.kduniv.aimong.feature.streak.data.StreakRepository
import com.kduniv.aimong.feature.streak.data.StreakRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindPetRepository(
        petRepositoryImpl: PetRepositoryImpl
    ): PetRepository

    @Binds
    @Singleton
    abstract fun bindGachaRepository(
        gachaRepositoryImpl: GachaRepositoryImpl
    ): GachaRepository

    @Binds
    @Singleton
    abstract fun bindStreakRepository(
        streakRepositoryImpl: StreakRepositoryImpl
    ): StreakRepository
}
