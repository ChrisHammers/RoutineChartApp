package com.HammersTech.RoutineChart.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineStepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: RoutineStepEntity)

    @Update
    suspend fun update(step: RoutineStepEntity)

    @Query("SELECT * FROM routine_steps WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RoutineStepEntity?

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId AND deletedAt IS NULL ORDER BY orderIndex ASC")
    suspend fun getByRoutineId(routineId: String): List<RoutineStepEntity>

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId AND deletedAt IS NULL ORDER BY orderIndex ASC")
    fun observeByRoutineId(routineId: String): Flow<List<RoutineStepEntity>>

    @Query("SELECT * FROM routine_steps WHERE familyId = :familyId AND deletedAt IS NULL")
    suspend fun getByFamilyId(familyId: String): List<RoutineStepEntity>
}

