package com.HammersTech.RoutineChart.app.di

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomChildProfileRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomCompletionEventRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomFamilyInviteRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomFamilyRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineStepRepository
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.CompletionEventRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineStepRepository
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
    // FamilyRepository is provided by FirebaseModule (CompositeFamilyRepository)
    // UserRepository is provided by FirebaseModule (CompositeUserRepository)
    // FamilyInviteRepository is provided by FirebaseModule (CompositeFamilyInviteRepository)

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
