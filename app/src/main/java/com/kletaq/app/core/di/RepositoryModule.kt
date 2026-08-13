package com.kletaq.app.core.di

import com.kletaq.app.data.repository.AuthRepository
import com.kletaq.app.data.repository.AuthRepositoryImpl
import com.kletaq.app.data.repository.LegalRepository
import com.kletaq.app.data.repository.LegalRepositoryImpl
import com.kletaq.app.data.repository.ProgressionRepository
import com.kletaq.app.data.repository.ProgressionRepositoryImpl
import com.kletaq.app.data.repository.SpacedRepetitionRepository
import com.kletaq.app.data.repository.SpacedRepetitionRepositoryImpl
import com.kletaq.app.data.repository.SyllabusRepository
import com.kletaq.app.data.repository.SyllabusRepositoryImpl
import com.kletaq.app.data.repository.UserRepository
import com.kletaq.app.data.repository.UserRepositoryImpl
import com.kletaq.app.data.repository.BacklogPlanRepository
import com.kletaq.app.data.repository.BacklogPlanRepositoryImpl
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindLegalRepository(impl: LegalRepositoryImpl): LegalRepository

    @Binds
    @Singleton
    abstract fun bindSyllabusRepository(impl: SyllabusRepositoryImpl): SyllabusRepository

    @Binds
    @Singleton
    abstract fun bindSpacedRepetitionRepository(impl: SpacedRepetitionRepositoryImpl): SpacedRepetitionRepository

    @Binds
    @Singleton
    abstract fun bindProgressionRepository(impl: ProgressionRepositoryImpl): ProgressionRepository

    @Binds
    @Singleton
    abstract fun bindBacklogPlanRepository(impl: BacklogPlanRepositoryImpl): BacklogPlanRepository
}
