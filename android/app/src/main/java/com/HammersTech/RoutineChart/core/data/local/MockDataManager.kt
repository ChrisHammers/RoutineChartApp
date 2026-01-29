package com.HammersTech.RoutineChart.core.data.local

import com.HammersTech.RoutineChart.core.domain.models.AgeBand
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.Family
import com.HammersTech.RoutineChart.core.domain.models.PlanTier
import com.HammersTech.RoutineChart.core.domain.models.ReadingMode
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
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
 * Manages mock/demo data for testing: family, children (Emma, Noah), Morning/Bedtime routines, and assignments.
 * Use for UI testing or demos. Not used automatically at app launch.
 */
@Singleton
class MockDataManager @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val childProfileRepository: ChildProfileRepository,
    private val routineRepository: RoutineRepository,
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val routineAssignmentRepository: RoutineAssignmentRepository,
    private val userRepository: UserRepository
) {
    /**
     * Inserts full mock data: family, 2 children (Emma, Noah), Morning Routine, Bedtime Routine, and 4 assignments.
     * Call explicitly when needed (e.g. debug menu). Does not check for existing routines.
     */
    suspend fun insertMockDataIfNeeded(userId: String, familyId: String? = null) = withContext(Dispatchers.IO) {
        AppLogger.Database.info("[Mock] insertMockDataIfNeeded entered, userId=$userId, familyId=$familyId")

        val family: Family
        val children: List<ChildProfile>

        if (familyId != null) {
            val existingFamily = familyRepository.getById(familyId)
            if (existingFamily != null) {
                family = existingFamily
                val existingChildren = childProfileRepository.getByFamilyId(familyId)
                children = if (existingChildren.isEmpty()) {
                    listOf(
                        ChildProfile(
                            id = UUID.randomUUID().toString(),
                            familyId = family.id,
                            displayName = "Emma",
                            avatarIcon = "🌟",
                            ageBand = AgeBand.AGE_5_7,
                            readingMode = ReadingMode.LIGHT_TEXT,
                            audioEnabled = true,
                            createdAt = Instant.now()
                        ),
                        ChildProfile(
                            id = UUID.randomUUID().toString(),
                            familyId = family.id,
                            displayName = "Noah",
                            avatarIcon = "🚀",
                            ageBand = AgeBand.AGE_8_10,
                            readingMode = ReadingMode.FULL_TEXT,
                            audioEnabled = true,
                            createdAt = Instant.now()
                        )
                    ).also { list ->
                        list.forEach { childProfileRepository.create(it) }
                    }
                } else {
                    existingChildren
                }
            } else {
                family = Family(
                    id = UUID.randomUUID().toString(),
                    name = "Test Family",
                    timeZone = "America/Los_Angeles",
                    weekStartsOn = 0,
                    planTier = PlanTier.FREE,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                familyRepository.create(family)
                children = listOf(
                    ChildProfile(
                        id = UUID.randomUUID().toString(),
                        familyId = family.id,
                        displayName = "Emma",
                        avatarIcon = "🌟",
                        ageBand = AgeBand.AGE_5_7,
                        readingMode = ReadingMode.LIGHT_TEXT,
                        audioEnabled = true,
                        createdAt = Instant.now()
                    ),
                    ChildProfile(
                        id = UUID.randomUUID().toString(),
                        familyId = family.id,
                        displayName = "Noah",
                        avatarIcon = "🚀",
                        ageBand = AgeBand.AGE_8_10,
                        readingMode = ReadingMode.FULL_TEXT,
                        audioEnabled = true,
                        createdAt = Instant.now()
                    )
                )
                children.forEach { childProfileRepository.create(it) }
            }
        } else {
            family = Family(
                id = UUID.randomUUID().toString(),
                name = "Test Family",
                timeZone = "America/Los_Angeles",
                weekStartsOn = 0,
                planTier = PlanTier.FREE,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            familyRepository.create(family)
            children = listOf(
                ChildProfile(
                    id = UUID.randomUUID().toString(),
                    familyId = family.id,
                    displayName = "Emma",
                    avatarIcon = "🌟",
                    ageBand = AgeBand.AGE_5_7,
                    readingMode = ReadingMode.LIGHT_TEXT,
                    audioEnabled = true,
                    createdAt = Instant.now()
                ),
                ChildProfile(
                    id = UUID.randomUUID().toString(),
                    familyId = family.id,
                    displayName = "Noah",
                    avatarIcon = "🚀",
                    ageBand = AgeBand.AGE_8_10,
                    readingMode = ReadingMode.FULL_TEXT,
                    audioEnabled = true,
                    createdAt = Instant.now()
                )
            )
            children.forEach { childProfileRepository.create(it) }
        }

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

        AppLogger.Database.info("[Mock] Mock data created: 1 family, 2 children, 2 routines, 10 steps, 4 assignments")
    }
}
