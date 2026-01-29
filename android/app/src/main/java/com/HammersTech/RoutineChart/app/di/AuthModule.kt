package com.HammersTech.RoutineChart.app.di

import com.HammersTech.RoutineChart.core.data.remote.firebase.FirebaseAuthService
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for authentication dependencies
 * Phase 2.1: Firebase Auth
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(firebaseAuthService: FirebaseAuthService): AuthRepository
}
