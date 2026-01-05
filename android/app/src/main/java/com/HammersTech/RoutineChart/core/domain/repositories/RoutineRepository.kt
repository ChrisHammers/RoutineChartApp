package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.Routine
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Routine operations
 */
interface RoutineRepository {
    suspend fun create(routine: Routine)
    suspend fun update(routine: Routine)
    suspend fun getById(id: String): Routine?
    fun observeById(id: String): Flow<Routine?>
    suspend fun getByFamilyId(familyId: String): List<Routine>
    fun observeByFamilyId(familyId: String): Flow<List<Routine>>
}

