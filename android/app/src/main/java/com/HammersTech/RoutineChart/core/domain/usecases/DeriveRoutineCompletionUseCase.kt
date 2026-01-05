package com.HammersTech.RoutineChart.core.domain.usecases

import com.HammersTech.RoutineChart.core.domain.models.CompletionRule
import com.HammersTech.RoutineChart.core.domain.models.Routine
import javax.inject.Inject

/**
 * Use case for deriving routine completion state
 * Checks if all steps meet the completion rule
 */
class DeriveRoutineCompletionUseCase @Inject constructor(
    private val deriveStepCompletionUseCase: DeriveStepCompletionUseCase
) {
    /**
     * Determine if a routine is complete for a given child and day
     * @return true if all steps are complete, false otherwise
     */
    suspend operator fun invoke(
        routine: Routine,
        childId: String,
        stepIds: List<String>,
        dayKey: String
    ): Boolean {
        return when (routine.completionRule) {
            CompletionRule.ALL_STEPS_REQUIRED -> {
                if (stepIds.isEmpty()) {
                    return false
                }

                val stepStates = deriveStepCompletionUseCase.getStepStates(
                    childId = childId,
                    routineId = routine.id,
                    stepIds = stepIds,
                    dayKey = dayKey
                )

                stepStates.values.all { it }
            }
        }
    }
}

