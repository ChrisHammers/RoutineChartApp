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

    // Phase 3.3: Query by userId OR familyId (if provided)
    @Query("""
        SELECT * FROM routines 
        WHERE (userId = :userId OR (:familyId IS NOT NULL AND familyId = :familyId))
        AND (:includeDeleted = 1 OR deletedAt IS NULL)
        ORDER BY createdAt DESC
    """)
    suspend fun getAll(userId: String, familyId: String?, includeDeleted: Boolean): List<RoutineEntity>

    // Phase 3.2: Upload Queue - Get unsynced routines (including soft-deleted so we sync deletedAt to Firestore)
    // Query by familyId if provided, otherwise by userId
    @Query("""
        SELECT * FROM routines 
        WHERE ((:familyId IS NOT NULL AND familyId = :familyId) OR (:familyId IS NULL AND userId = :userId))
        AND synced = 0
    """)
    suspend fun getUnsynced(familyId: String?, userId: String): List<RoutineEntity>

    // Phase 3.2: Upload Queue - Mark as synced
    @Query("UPDATE routines SET synced = 1 WHERE id = :routineId")
    suspend fun markAsSynced(routineId: String)

    // Phase 3.2: Upload Queue - Mark multiple as synced
    @Query("UPDATE routines SET synced = 1 WHERE id IN (:routineIds)")
    suspend fun markAsSynced(routineIds: List<String>)
}

