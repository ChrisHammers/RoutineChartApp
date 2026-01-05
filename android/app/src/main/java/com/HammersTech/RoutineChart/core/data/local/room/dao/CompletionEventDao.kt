package com.HammersTech.RoutineChart.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.HammersTech.RoutineChart.core.data.local.room.entities.CompletionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CompletionEventEntity)

    @Query("SELECT * FROM completion_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CompletionEventEntity?

    @Query("SELECT * FROM completion_events WHERE childId = :childId AND localDayKey = :dayKey ORDER BY eventAt ASC, id ASC")
    suspend fun getByChildAndDay(childId: String, dayKey: String): List<CompletionEventEntity>

    @Query("SELECT * FROM completion_events WHERE childId = :childId AND localDayKey = :dayKey ORDER BY eventAt ASC, id ASC")
    fun observeByChildAndDay(childId: String, dayKey: String): Flow<List<CompletionEventEntity>>

    @Query("SELECT * FROM completion_events WHERE childId = :childId AND routineId = :routineId AND stepId = :stepId AND localDayKey = :dayKey ORDER BY eventAt ASC, id ASC")
    suspend fun getByStep(childId: String, routineId: String, stepId: String, dayKey: String): List<CompletionEventEntity>

    @Query("SELECT * FROM completion_events WHERE familyId = :familyId AND localDayKey = :dayKey")
    suspend fun getByFamilyAndDay(familyId: String, dayKey: String): List<CompletionEventEntity>

    @Query("SELECT * FROM completion_events WHERE synced = 0 ORDER BY eventAt ASC LIMIT :limit")
    suspend fun getUnsyncedEvents(limit: Int): List<CompletionEventEntity>

    @Query("UPDATE completion_events SET synced = 1 WHERE id IN (:eventIds)")
    suspend fun markAsSynced(eventIds: List<String>)
}

