package com.HammersTech.RoutineChart.core.data.local

import com.HammersTech.RoutineChart.core.domain.models.AgeBand
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.Family
import com.HammersTech.RoutineChart.core.domain.models.PlanTier
import com.HammersTech.RoutineChart.core.domain.models.ReadingMode
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.domain.usecases.CreateRoutineUseCase
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages seeding initial data for testing
 */
@Singleton
class SeedDataManager @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val childProfileRepository: ChildProfileRepository,
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val routineAssignmentRepository: RoutineAssignmentRepository,
    private val userRepository: com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
) {
    suspend fun seedDataIfNeeded(userId: String) = withContext(Dispatchers.IO) {
        // Check if already seeded
        val existingFamily = familyRepository.getFirst()
        if (existingFamily != null) {
            AppLogger.Database.info("Data already seeded, skipping")
            return@withContext
        }

        AppLogger.Database.info("Seeding initial data...")

        // Create family
        val family = Family(
            id = UUID.randomUUID().toString(),
            name = "Test Family",
            timeZone = "America/Los_Angeles",
            weekStartsOn = 0,
            planTier = PlanTier.FREE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        familyRepository.create(family)

        // Create two children
        val emma = ChildProfile(
            id = UUID.randomUUID().toString(),
            familyId = family.id,
            displayName = "Emma",
            avatarIcon = "🌟",
            ageBand = AgeBand.AGE_5_7,
            readingMode = ReadingMode.LIGHT_TEXT,
            audioEnabled = true,
            createdAt = Instant.now()
        )
        childProfileRepository.create(emma)

        val noah = ChildProfile(
            id = UUID.randomUUID().toString(),
            familyId = family.id,
            displayName = "Noah",
            avatarIcon = "🚀",
            ageBand = AgeBand.AGE_8_10,
            readingMode = ReadingMode.FULL_TEXT,
            audioEnabled = true,
            createdAt = Instant.now()
        )
        childProfileRepository.create(noah)

        // Create Morning Routine
        val morningRoutine = createRoutineUseCase(
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

        // Create Bedtime Routine
        val bedtimeRoutine = createRoutineUseCase(
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

        // Assign both routines to both children
        val children = listOf(emma, noah)
        val routines = listOf(morningRoutine, bedtimeRoutine)

        for (child in children) {
            for (routine in routines) {
                val assignment = com.HammersTech.RoutineChart.core.domain.models.RoutineAssignment(
                    id = UUID.randomUUID().toString(),
                    familyId = family.id,
                    routineId = routine.id,
                    childId = child.id,
                    isActive = true,
                    assignedAt = Instant.now(),
                    deletedAt = null
                )
                routineAssignmentRepository.create(assignment)
            }
        }

        AppLogger.Database.info("Seed data created: 1 family, 2 children, 2 routines, 10 steps, 4 assignments")
    }
}

