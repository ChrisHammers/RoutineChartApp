package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for User operations
 */
interface UserRepository {
    suspend fun create(user: User)
    suspend fun update(user: User)
    suspend fun getById(id: String): User?
    fun observeById(id: String): Flow<User?>
    suspend fun getByFamilyId(familyId: String): List<User>
    fun observeByFamilyId(familyId: String): Flow<List<User>>
}

