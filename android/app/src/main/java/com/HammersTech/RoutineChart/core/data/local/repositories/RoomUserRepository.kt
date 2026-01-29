package com.HammersTech.RoutineChart.core.data.local.repositories

import com.HammersTech.RoutineChart.core.data.local.room.dao.UserDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.UserEntity
import com.HammersTech.RoutineChart.core.domain.models.User
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room implementation of UserRepository
 */
class RoomUserRepository
    @Inject
    constructor(
        private val userDao: UserDao,
    ) : UserRepository {
        override suspend fun create(user: User) {
            userDao.insert(UserEntity.fromDomain(user))
            AppLogger.Database.info("Created user: ${user.id}")
        }

        override suspend fun update(user: User) {
            userDao.update(UserEntity.fromDomain(user))
            AppLogger.Database.info("Updated user: ${user.id}")
        }

        override suspend fun getById(id: String): User? {
            return userDao.getById(id)?.toDomain()
        }

        override fun observeById(id: String): Flow<User?> {
            return userDao.observeById(id).map { it?.toDomain() }
        }

        override suspend fun getByFamilyId(familyId: String): List<User> {
            return userDao.getByFamilyId(familyId).map { it.toDomain() }
        }

        override fun observeByFamilyId(familyId: String): Flow<List<User>> {
            return userDao.observeByFamilyId(familyId).map { list ->
                list.map { it.toDomain() }
            }
        }

        override suspend fun updateFamilyId(
            userId: String,
            familyId: String,
        ) {
            val user =
                userDao.getById(userId)
                    ?: throw IllegalStateException("User not found: $userId")

            val updatedUser = user.copy(familyId = familyId)
            userDao.update(updatedUser)
            AppLogger.Database.info("Updated user $userId familyId to $familyId")
        }
    }
