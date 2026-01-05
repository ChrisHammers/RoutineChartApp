package com.HammersTech.RoutineChart.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: RoutineEntity)

    @Update
    suspend fun update(routine: RoutineEntity)

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RoutineEntity?

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<RoutineEntity?>

    @Query("SELECT * FROM routines WHERE familyId = :familyId AND deletedAt IS NULL")
    suspend fun getByFamilyId(familyId: String): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE familyId = :familyId AND deletedAt IS NULL")
    fun observeByFamilyId(familyId: String): Flow<List<RoutineEntity>>
}

