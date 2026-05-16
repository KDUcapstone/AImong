package com.kduniv.aimong.core.di

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.privacy.PrivacyRepository
import com.kduniv.aimong.core.privacy.PrivacyRepositoryImpl
import com.kduniv.aimong.core.privacy.PrivacyRepositoryStub
import com.kduniv.aimong.feature.chat.data.ChatRepositoryImpl
import com.kduniv.aimong.feature.chat.domain.repository.ChatRepository
import com.kduniv.aimong.feature.dev.mock.repository.ChatRepositoryStub
import com.kduniv.aimong.feature.dev.mock.repository.GachaRepositoryStub
import com.kduniv.aimong.feature.dev.mock.repository.PetRepositoryStub
import com.kduniv.aimong.feature.dev.mock.repository.QuestRepositoryStub
import com.kduniv.aimong.feature.dev.mock.repository.QuizRepositoryStub
import com.kduniv.aimong.feature.dev.mock.repository.WalletRepositoryStub
import com.kduniv.aimong.feature.wallet.domain.repository.WalletRepository
import com.kduniv.aimong.feature.wallet.data.WalletRepositoryImpl
import com.kduniv.aimong.feature.gacha.data.GachaRepository
import com.kduniv.aimong.feature.gacha.data.GachaRepositoryImpl
import com.kduniv.aimong.feature.home.data.PetRepository
import com.kduniv.aimong.feature.home.data.PetRepositoryImpl
import com.kduniv.aimong.feature.quest.data.QuestRepositoryImpl
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import com.kduniv.aimong.feature.quiz.data.QuizRepositoryImpl
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * [UiMode.useStubNav] 일 때 자녀 목업 플로우에서 Retrofit 호출을 피하기 위한 구현체 분기.
 */
@Module
@InstallIn(SingletonComponent::class)
object StubRepositoryModule {

    @Provides
    @Singleton
    fun provideQuestRepository(
        impl: QuestRepositoryImpl,
        stub: QuestRepositoryStub
    ): QuestRepository = if (UiMode.useStubNav) stub else impl

    @Provides
    @Singleton
    fun provideQuizRepository(
        impl: QuizRepositoryImpl,
        stub: QuizRepositoryStub
    ): QuizRepository = if (UiMode.useStubNav) stub else impl

    @Provides
    @Singleton
    fun provideChatRepository(
        impl: ChatRepositoryImpl,
        stub: ChatRepositoryStub
    ): ChatRepository = if (UiMode.useStubNav) stub else impl

    @Provides
    @Singleton
    fun provideGachaRepository(
        impl: GachaRepositoryImpl,
        stub: GachaRepositoryStub
    ): GachaRepository = if (UiMode.useStubNav) stub else impl

    @Provides
    @Singleton
    fun providePetRepository(
        impl: PetRepositoryImpl,
        stub: PetRepositoryStub
    ): PetRepository = if (UiMode.useStubNav) stub else impl

    @Provides
    @Singleton
    fun providePrivacyRepository(
        impl: PrivacyRepositoryImpl,
        stub: PrivacyRepositoryStub
    ): PrivacyRepository = if (UiMode.useStubNav) stub else impl

    @Provides
    @Singleton
    fun provideWalletRepository(
        impl: WalletRepositoryImpl,
        stub: WalletRepositoryStub
    ): WalletRepository = if (UiMode.useStubNav) stub else impl
}
