package com.HammersTech.RoutineChart.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineAssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: RoutineAssignmentEntity)

    @Update
    suspend fun update(assignment: RoutineAssignmentEntity)

    @Query("SELECT * FROM routine_assignments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RoutineAssignmentEntity?

    @Query("SELECT * FROM routine_assignments WHERE familyId = :familyId AND deletedAt IS NULL")
    suspend fun getByFamilyId(familyId: String): List<RoutineAssignmentEntity>

    @Query("SELECT * FROM routine_assignments WHERE childId = :childId AND isActive = 1 AND deletedAt IS NULL")
    suspend fun getActiveByChildId(childId: String): List<RoutineAssignmentEntity>

    @Query("SELECT * FROM routine_assignments WHERE childId = :childId AND isActive = 1 AND deletedAt IS NULL")
    fun observeActiveByChildId(childId: String): Flow<List<RoutineAssignmentEntity>>

    @Query("SELECT * FROM routine_assignments WHERE routineId = :routineId AND deletedAt IS NULL")
    suspend fun getByRoutineId(routineId: String): List<RoutineAssignmentEntity>
}
