package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.RoutineAssignment
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for RoutineAssignment operations
 */
interface RoutineAssignmentRepository {
    suspend fun create(assignment: RoutineAssignment)

    suspend fun update(assignment: RoutineAssignment)

    suspend fun getById(id: String): RoutineAssignment?

    suspend fun getByFamilyId(familyId: String): List<RoutineAssignment>

    suspend fun getActiveByChildId(childId: String): List<RoutineAssignment>

    fun observeActiveByChildId(childId: String): Flow<List<RoutineAssignment>>

    suspend fun getByRoutineId(routineId: String): List<RoutineAssignment>
}
