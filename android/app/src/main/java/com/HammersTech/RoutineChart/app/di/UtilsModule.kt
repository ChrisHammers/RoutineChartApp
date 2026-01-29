package com.HammersTech.RoutineChart.app.di

import android.content.Context
import com.HammersTech.RoutineChart.core.utils.DeviceIdentifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for utility dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {
    @Provides
    @Singleton
    fun provideDeviceIdentifier(
        @ApplicationContext context: Context,
    ): DeviceIdentifier {
        return DeviceIdentifier(context)
    }
}
