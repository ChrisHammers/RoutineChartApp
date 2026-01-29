package com.HammersTech.RoutineChart.core.data.local.repositories

import com.HammersTech.RoutineChart.core.data.local.room.dao.CompletionEventDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.CompletionEventEntity
import com.HammersTech.RoutineChart.core.domain.models.CompletionEvent
import com.HammersTech.RoutineChart.core.domain.repositories.CompletionEventRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room implementation of CompletionEventRepository
 */
class RoomCompletionEventRepository
    @Inject
    constructor(
        private val completionEventDao: CompletionEventDao,
    ) : CompletionEventRepository {
        override suspend fun create(event: CompletionEvent) {
            completionEventDao.insert(CompletionEventEntity.fromDomain(event))
            AppLogger.Database.info("Created completion event: ${event.id} (${event.eventType})")
        }

        override suspend fun getById(id: String): CompletionEvent? {
            return completionEventDao.getById(id)?.toDomain()
        }

        override suspend fun getByChildAndDay(
            childId: String,
            dayKey: String,
        ): List<CompletionEvent> {
            return completionEventDao.getByChildAndDay(childId, dayKey).map { it.toDomain() }
        }

        override fun observeByChildAndDay(
            childId: String,
            dayKey: String,
        ): Flow<List<CompletionEvent>> {
            return completionEventDao.observeByChildAndDay(childId, dayKey).map { list ->
                list.map { it.toDomain() }
            }
        }

        override suspend fun getByStep(
            childId: String,
            routineId: String,
            stepId: String,
            dayKey: String,
        ): List<CompletionEvent> {
            return completionEventDao.getByStep(childId, routineId, stepId, dayKey).map { it.toDomain() }
        }

        override suspend fun getByFamilyAndDay(
            familyId: String,
            dayKey: String,
        ): List<CompletionEvent> {
            return completionEventDao.getByFamilyAndDay(familyId, dayKey).map { it.toDomain() }
        }

        override suspend fun getUnsyncedEvents(limit: Int): List<CompletionEvent> {
            return completionEventDao.getUnsyncedEvents(limit).map { it.toDomain() }
        }

        override suspend fun markAsSynced(eventIds: List<String>) {
            completionEventDao.markAsSynced(eventIds)
            AppLogger.Database.info("Marked ${eventIds.size} events as synced")
        }
    }
