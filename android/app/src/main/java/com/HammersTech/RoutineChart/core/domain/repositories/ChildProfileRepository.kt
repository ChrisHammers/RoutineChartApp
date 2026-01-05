package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ChildProfile operations
 */
interface ChildProfileRepository {
    suspend fun create(profile: ChildProfile)
    suspend fun update(profile: ChildProfile)
    suspend fun getById(id: String): ChildProfile?
    fun observeById(id: String): Flow<ChildProfile?>
    suspend fun getByFamilyId(familyId: String): List<ChildProfile>
    fun observeByFamilyId(familyId: String): Flow<List<ChildProfile>>
}

