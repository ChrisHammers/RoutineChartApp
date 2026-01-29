package com.HammersTech.RoutineChart.app.di

import android.content.Context
import androidx.room.Room
import com.HammersTech.RoutineChart.core.data.local.room.RoutineChartDatabase
import com.HammersTech.RoutineChart.core.data.local.room.dao.ChildProfileDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.CompletionEventDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.FamilyDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineAssignmentDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineStepDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.UserDao
import com.HammersTech.RoutineChart.core.data.local.room.daos.FamilyInviteDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.SyncCursorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database and DAO dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoutineChartDatabase {
        return Room.databaseBuilder(
            context,
            RoutineChartDatabase::class.java,
            RoutineChartDatabase.DATABASE_NAME
        )
            .addMigrations(
                com.HammersTech.RoutineChart.core.data.local.room.migrations.MIGRATION_2_3,
                com.HammersTech.RoutineChart.core.data.local.room.migrations.MIGRATION_3_4,
                com.HammersTech.RoutineChart.core.data.local.room.migrations.MIGRATION_4_5,
                com.HammersTech.RoutineChart.core.data.local.room.migrations.MIGRATION_5_6,
                com.HammersTech.RoutineChart.core.data.local.room.migrations.MIGRATION_6_7,
                com.HammersTech.RoutineChart.core.data.local.room.migrations.MIGRATION_7_8,
                com.HammersTech.RoutineChart.core.data.local.room.migrations.MIGRATION_8_9
            )
            .fallbackToDestructiveMigration() // Phase 1: Dev only - remove in production
            .build()
    }

    @Provides
    fun provideFamilyDao(database: RoutineChartDatabase): FamilyDao {
        return database.familyDao()
    }

    @Provides
    fun provideUserDao(database: RoutineChartDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideChildProfileDao(database: RoutineChartDatabase): ChildProfileDao {
        return database.childProfileDao()
    }

    @Provides
    fun provideRoutineDao(database: RoutineChartDatabase): RoutineDao {
        return database.routineDao()
    }

    @Provides
    fun provideRoutineStepDao(database: RoutineChartDatabase): RoutineStepDao {
        return database.routineStepDao()
    }

    @Provides
    fun provideRoutineAssignmentDao(database: RoutineChartDatabase): RoutineAssignmentDao {
        return database.routineAssignmentDao()
    }

    @Provides
    fun provideCompletionEventDao(database: RoutineChartDatabase): CompletionEventDao {
        return database.completionEventDao()
    }

    @Provides
    fun provideFamilyInviteDao(database: RoutineChartDatabase): FamilyInviteDao {
        return database.familyInviteDao()
    }

    @Provides
    fun provideSyncCursorDao(database: RoutineChartDatabase): SyncCursorDao {
        return database.syncCursorDao()
    }
}

