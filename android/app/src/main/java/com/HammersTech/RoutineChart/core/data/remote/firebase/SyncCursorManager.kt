package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.room.dao.SyncCursorDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.SyncCursorEntity
import com.HammersTech.RoutineChart.core.domain.models.SyncCursor
import com.HammersTech.RoutineChart.core.utils.AppLogger
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sync cursors for tracking last sync timestamp per collection
 * Phase 3.1: Sync Infrastructure
 */
@Singleton
class SyncCursorManager
    @Inject
    constructor(
        private val syncCursorDao: SyncCursorDao,
    ) {
        /**
         * Get the last sync timestamp for a collection
         * Returns null if no cursor exists (first sync)
         */
        suspend fun getCursor(collection: String): SyncCursor? {
            val entity = syncCursorDao.getByCollection(collection)
            AppLogger.Database.info("Getting cursor for collection '$collection': ${if (entity != null) "found" else "not found"}")
            return entity?.toDomain()
        }

        /**
         * Update or create a cursor for a collection
         */
        suspend fun updateCursor(
            collection: String,
            lastSyncedAt: Instant,
        ) {
            val entity = SyncCursorEntity.fromDomain(collection, lastSyncedAt)
            syncCursorDao.insertOrUpdate(entity)
            AppLogger.Database.info("Updated sync cursor for collection '$collection' to $lastSyncedAt")
        }

        /**
         * Delete a cursor (useful for resetting sync state)
         */
        suspend fun deleteCursor(collection: String) {
            syncCursorDao.deleteByCollection(collection)
            AppLogger.Database.info("Deleted sync cursor for collection '$collection'")
        }

        /**
         * Get all cursors (for debugging/monitoring)
         */
        suspend fun getAllCursors(): List<SyncCursor> {
            val entities = syncCursorDao.getAll()
            AppLogger.Database.info("Getting all cursors: found ${entities.size} cursor(s)")
            entities.forEach { entity ->
                AppLogger.Database.info("  - ${entity.collection}: ${entity.lastSyncedAt}")
            }
            return entities.map { it.toDomain() }
        }
    }
