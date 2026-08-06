package com.studyos.app.core.di

import com.studyos.app.data.repository.AuthRepository
import com.studyos.app.data.repository.AuthRepositoryImpl
import com.studyos.app.data.repository.LegalRepository
import com.studyos.app.data.repository.LegalRepositoryImpl
import com.studyos.app.data.repository.ProgressionRepository
import com.studyos.app.data.repository.ProgressionRepositoryImpl
import com.studyos.app.data.repository.SpacedRepetitionRepository
import com.studyos.app.data.repository.SpacedRepetitionRepositoryImpl
import com.studyos.app.data.repository.SyllabusRepository
import com.studyos.app.data.repository.SyllabusRepositoryImpl
import com.studyos.app.data.repository.UserRepository
import com.studyos.app.data.repository.UserRepositoryImpl
import com.studyos.app.data.repository.BacklogPlanRepository
import com.studyos.app.data.repository.BacklogPlanRepositoryImpl
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
