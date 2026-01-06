package com.HammersTech.RoutineChart.core.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.HammersTech.RoutineChart.core.data.local.room.dao.ChildProfileDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.CompletionEventDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.FamilyDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineAssignmentDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineStepDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.UserDao
import com.HammersTech.RoutineChart.core.data.local.room.daos.FamilyInviteDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.ChildProfileEntity
import com.HammersTech.RoutineChart.core.data.local.room.entities.CompletionEventEntity
import com.HammersTech.RoutineChart.core.data.local.room.entities.FamilyEntity
import com.HammersTech.RoutineChart.core.data.local.room.entities.FamilyInviteEntity
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineAssignmentEntity
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineEntity
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineStepEntity
import com.HammersTech.RoutineChart.core.data.local.room.entities.UserEntity

/**
 * Room database for Routine Chart App
 * Version 1: Initial schema with all core entities
 * Version 2: Added FamilyInviteEntity (Phase 2.2: QR Family Joining)
 * Version 3: Added inviteCode column to FamilyInviteEntity
 */
@Database(
    entities = [
        FamilyEntity::class,
        UserEntity::class,
        ChildProfileEntity::class,
        RoutineEntity::class,
        RoutineStepEntity::class,
        RoutineAssignmentEntity::class,
        CompletionEventEntity::class,
        FamilyInviteEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RoutineChartDatabase : RoomDatabase() {
    abstract fun familyDao(): FamilyDao
    abstract fun userDao(): UserDao
    abstract fun childProfileDao(): ChildProfileDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineStepDao(): RoutineStepDao
    abstract fun routineAssignmentDao(): RoutineAssignmentDao
    abstract fun completionEventDao(): CompletionEventDao
    abstract fun familyInviteDao(): FamilyInviteDao

    companion object {
        const val DATABASE_NAME = "routine_chart.db"
    }
}

