package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.Family
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Family operations
 */
interface FamilyRepository {
    suspend fun create(family: Family)

    suspend fun update(family: Family)

    suspend fun getById(id: String): Family?

    fun observeById(id: String): Flow<Family?>

    suspend fun getFirst(): Family?

    suspend fun getAll(): List<Family>
}
