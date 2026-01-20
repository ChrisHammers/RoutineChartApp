package com.HammersTech.RoutineChart.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.HammersTech.RoutineChart.core.data.local.room.entities.SyncCursorEntity

/**
 * DAO for sync cursor operations
 * Phase 3.1: Sync Infrastructure
 */
@Dao
interface SyncCursorDao {
    @Query("SELECT * FROM sync_cursors WHERE collection = :collection LIMIT 1")
    suspend fun getByCollection(collection: String): SyncCursorEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cursor: SyncCursorEntity)
    
    @Query("DELETE FROM sync_cursors WHERE collection = :collection")
    suspend fun deleteByCollection(collection: String)
    
    @Query("SELECT * FROM sync_cursors")
    suspend fun getAll(): List<SyncCursorEntity>
}
