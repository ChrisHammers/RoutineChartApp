package com.HammersTech.RoutineChart.app.di

import com.HammersTech.RoutineChart.core.domain.repositories.FamilyInviteRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeFamilyInviteRepository
import com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeFamilyRepository
import com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing Firebase/Firestore dependencies
 * Phase 2.3: Firestore Sync
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {
    
    @Binds
    @Singleton
    abstract fun bindFamilyRepository(
        impl: CompositeFamilyRepository
    ): FamilyRepository
    
    @Binds
    @Singleton
    abstract fun bindFamilyInviteRepository(
        impl: CompositeFamilyInviteRepository
    ): FamilyInviteRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: CompositeUserRepository
    ): UserRepository
}
