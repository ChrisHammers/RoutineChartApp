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
    
    /**
     * Update user's family ID (for joining a family)
     * Phase 2.3.1: User Linking
     */
    suspend fun updateFamilyId(userId: String, familyId: String)
}

