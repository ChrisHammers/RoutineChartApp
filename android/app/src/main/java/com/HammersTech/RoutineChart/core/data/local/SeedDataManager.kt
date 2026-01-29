package com.HammersTech.RoutineChart.core.data.local

import com.HammersTech.RoutineChart.core.domain.models.Family
import com.HammersTech.RoutineChart.core.domain.models.PlanTier
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.domain.usecases.CreateRoutineUseCase
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages seeding initial routines (Morning, Bedtime) only when the user has no routines.
 * Does not add children; parent can add children and assign routines later.
 */
@Singleton
class SeedDataManager @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val routineRepository: RoutineRepository,
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val userRepository: UserRepository
) {
    /**
     * Seeds demo routines (Morning, Bedtime) only when the user has no routines.
     * Call after sync so local DB may already have routines from Firestore.
     * When [familyId] is provided and the family exists, seeds into that family so new routines sync to the cloud.
     * Does not create children or assignments.
     */
    suspend fun seedDataIfNeeded(userId: String, familyId: String? = null) = withContext(Dispatchers.IO) {
        AppLogger.Database.info("[Seed] seedDataIfNeeded entered, userId=$userId, familyId=$familyId")
        val existingRoutines = routineRepository.getAll(userId, familyId = null, includeDeleted = false)
        AppLogger.Database.info("[Seed] existingRoutines count=${existingRoutines.size}, ids=${existingRoutines.map { it.id }}, titles=${existingRoutines.map { it.title }}")
        if (existingRoutines.isNotEmpty()) {
            AppLogger.Database.info("[Seed] User already has routines, skipping seed")
            return@withContext
        }

        AppLogger.Database.info("[Seed] No routines found, seeding initial data (routines only, no children)...")

        val family: Family = if (familyId != null) {
            familyRepository.getById(familyId)
                ?: run {
                    val newFamily = Family(
                        id = UUID.randomUUID().toString(),
                        name = "Test Family",
                        timeZone = "America/Los_Angeles",
                        weekStartsOn = 0,
                        planTier = PlanTier.FREE,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                    familyRepository.create(newFamily)
                    newFamily
                }
        } else {
            val newFamily = Family(
                id = UUID.randomUUID().toString(),
                name = "Test Family",
                timeZone = "America/Los_Angeles",
                weekStartsOn = 0,
                planTier = PlanTier.FREE,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            familyRepository.create(newFamily)
            newFamily
        }

        createRoutineUseCase(
            userId = userId,
            familyId = family.id,
            title = "Morning Routine",
            iconName = "☀️",
            steps = listOf(
                CreateRoutineUseCase.StepInput("Wake up", "🛏️"),
                CreateRoutineUseCase.StepInput("Brush teeth", "🦷"),
                CreateRoutineUseCase.StepInput("Get dressed", "👕"),
                CreateRoutineUseCase.StepInput("Eat breakfast", "🍳"),
                CreateRoutineUseCase.StepInput("Pack backpack", "🎒")
            )
        )

        createRoutineUseCase(
            userId = userId,
            familyId = family.id,
            title = "Bedtime Routine",
            iconName = "🌙",
            steps = listOf(
                CreateRoutineUseCase.StepInput("Put on pajamas", "👘"),
                CreateRoutineUseCase.StepInput("Brush teeth", "🦷"),
                CreateRoutineUseCase.StepInput("Read a book", "📚"),
                CreateRoutineUseCase.StepInput("Say goodnight", "💤"),
                CreateRoutineUseCase.StepInput("Lights out", "🌃")
            )
        )

        AppLogger.Database.info("[Seed] Seed data created: 1 family, 2 routines, 10 steps (no children)")
    }
}
