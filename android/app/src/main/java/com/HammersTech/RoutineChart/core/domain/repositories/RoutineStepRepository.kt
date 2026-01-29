package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for RoutineStep operations
 */
interface RoutineStepRepository {
    suspend fun create(step: RoutineStep)

    suspend fun update(step: RoutineStep)

    suspend fun getById(id: String): RoutineStep?

    suspend fun getByRoutineId(routineId: String): List<RoutineStep>

    fun observeByRoutineId(routineId: String): Flow<List<RoutineStep>>
    // Note: getByFamilyId removed - RoutineStep no longer has familyId
    // Steps are accessed via their parent Routine (getByRoutineId)
}
