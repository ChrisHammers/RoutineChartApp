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

    // Phase 3.4: Upload Queue - Get unsynced steps
    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId AND synced = 0 AND deletedAt IS NULL ORDER BY orderIndex ASC")
    suspend fun getUnsynced(routineId: String): List<RoutineStepEntity>

    // Phase 3.4: Upload Queue - Mark as synced
    @Query("UPDATE routine_steps SET synced = 1 WHERE id = :stepId")
    suspend fun markAsSynced(stepId: String)

    // Phase 3.4: Upload Queue - Mark multiple as synced
    @Query("UPDATE routine_steps SET synced = 1 WHERE id IN (:stepIds)")
    suspend fun markAsSynced(stepIds: List<String>)
}
