package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Entity for tracking sync cursors (last sync timestamp per collection)
 * Phase 3.1: Sync Infrastructure
 */
@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(
    @PrimaryKey
    val id: String, // collection name (e.g., "routines", "events")
    val collection: String,
    val lastSyncedAt: Instant
) {
    companion object {
        fun fromDomain(collection: String, lastSyncedAt: Instant): SyncCursorEntity {
            return SyncCursorEntity(
                id = collection,
                collection = collection,
                lastSyncedAt = lastSyncedAt
            )
        }
    }
    
    fun toDomain(): com.HammersTech.RoutineChart.core.domain.models.SyncCursor {
        return com.HammersTech.RoutineChart.core.domain.models.SyncCursor(
            collection = collection,
            lastSyncedAt = lastSyncedAt
        )
    }
}
