package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.CompletionEvent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for CompletionEvent operations
 */
interface CompletionEventRepository {
    suspend fun create(event: CompletionEvent)
    suspend fun getById(id: String): CompletionEvent?
    suspend fun getByChildAndDay(childId: String, dayKey: String): List<CompletionEvent>
    fun observeByChildAndDay(childId: String, dayKey: String): Flow<List<CompletionEvent>>
    suspend fun getByStep(childId: String, routineId: String, stepId: String, dayKey: String): List<CompletionEvent>
    suspend fun getByFamilyAndDay(familyId: String, dayKey: String): List<CompletionEvent>
    suspend fun getUnsyncedEvents(limit: Int): List<CompletionEvent>
    suspend fun markAsSynced(eventIds: List<String>)
}

