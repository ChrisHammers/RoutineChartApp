package com.HammersTech.RoutineChart.app.di

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomChildProfileRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomCompletionEventRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomFamilyRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineStepRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomUserRepository
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.CompletionEventRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineStepRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFamilyRepository(
        impl: RoomFamilyRepository
    ): FamilyRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: RoomUserRepository
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindChildProfileRepository(
        impl: RoomChildProfileRepository
    ): ChildProfileRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(
        impl: RoomRoutineRepository
    ): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindRoutineStepRepository(
        impl: RoomRoutineStepRepository
    ): RoutineStepRepository

    @Binds
    @Singleton
    abstract fun bindRoutineAssignmentRepository(
        impl: RoomRoutineAssignmentRepository
    ): RoutineAssignmentRepository

    @Binds
    @Singleton
    abstract fun bindCompletionEventRepository(
        impl: RoomCompletionEventRepository
    ): CompletionEventRepository
}

