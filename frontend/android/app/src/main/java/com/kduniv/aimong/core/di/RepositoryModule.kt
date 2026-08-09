package com.kduniv.aimong.core.di

import com.kduniv.aimong.feature.auth.data.AuthRepositoryImpl
import com.kduniv.aimong.feature.auth.data.AuthRepository
import com.kduniv.aimong.feature.home.data.AppBootstrapRepositoryImpl
import com.kduniv.aimong.feature.home.data.HomeRepositoryImpl
import com.kduniv.aimong.feature.home.domain.repository.AppBootstrapRepository
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import com.kduniv.aimong.feature.mission.data.MissionRepositoryImpl
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import com.kduniv.aimong.feature.streak.data.StreakRepository
import com.kduniv.aimong.feature.streak.data.StreakRepositoryImpl
import com.kduniv.aimong.feature.settings.data.NotificationSettingsRepository
import com.kduniv.aimong.feature.settings.data.NotificationSettingsRepositoryImpl
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
    abstract fun bindAppBootstrapRepository(
        impl: AppBootstrapRepositoryImpl
    ): AppBootstrapRepository

    @Binds
    @Singleton
    abstract fun bindMissionRepository(
        missionRepositoryImpl: MissionRepositoryImpl
    ): MissionRepository

    @Binds
    @Singleton
    abstract fun bindStreakRepository(
        streakRepositoryImpl: StreakRepositoryImpl
    ): StreakRepository

    @Binds
    @Singleton
    abstract fun bindNotificationSettingsRepository(
        impl: NotificationSettingsRepositoryImpl
    ): NotificationSettingsRepository
}
