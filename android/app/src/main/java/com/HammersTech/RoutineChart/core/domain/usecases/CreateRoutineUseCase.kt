package com.HammersTech.RoutineChart.core.domain.usecases

import com.HammersTech.RoutineChart.core.domain.models.CompletionRule
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineStepRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for creating a routine with steps
 */
class CreateRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val routineStepRepository: RoutineStepRepository
) {
    data class StepInput(
        val label: String?,
        val iconName: String?,
        val audioCueUrl: String? = null
    )

    suspend operator fun invoke(
        userId: String,
        familyId: String? = null,
        title: String,
        iconName: String?,
        steps: List<StepInput>
    ): Routine {
        val routineId = UUID.randomUUID().toString()
        val now = Instant.now()

        // Create routine
        val routine = Routine(
            id = routineId,
            userId = userId,
            familyId = familyId,
            title = title,
            iconName = iconName,
            version = Routine.DEFAULT_VERSION,
            completionRule = CompletionRule.ALL_STEPS_REQUIRED,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )

        routineRepository.create(routine)

        // Create steps (no familyId needed)
        steps.forEachIndexed { index, stepInput ->
            val step = RoutineStep(
                id = UUID.randomUUID().toString(),
                routineId = routineId,
                orderIndex = index,
                label = stepInput.label,
                iconName = stepInput.iconName,
                audioCueUrl = stepInput.audioCueUrl,
                createdAt = now,
                deletedAt = null
            )
            routineStepRepository.create(step)
        }

        AppLogger.UseCase.info("Created routine: $title with ${steps.size} steps")
        return routine
    }
}

