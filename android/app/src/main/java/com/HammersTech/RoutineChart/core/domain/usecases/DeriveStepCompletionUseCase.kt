package com.HammersTech.RoutineChart.core.domain.usecases

import com.HammersTech.RoutineChart.core.domain.models.CompletionEvent
import com.HammersTech.RoutineChart.core.domain.models.EventType
import com.HammersTech.RoutineChart.core.domain.repositories.CompletionEventRepository
import javax.inject.Inject

/**
 * Use case for deriving step completion state from event log
 * Implements event-sourcing: last event type determines current state
 */
class DeriveStepCompletionUseCase
    @Inject
    constructor(
        private val completionEventRepository: CompletionEventRepository,
    ) {
        /**
         * Determine if a step is complete for a given child and day
         * @return true if last event type is COMPLETE, false otherwise
         */
        suspend operator fun invoke(
            childId: String,
            routineId: String,
            stepId: String,
            dayKey: String,
        ): Boolean {
            val events =
                completionEventRepository.getByStep(
                    childId = childId,
                    routineId = routineId,
                    stepId = stepId,
                    dayKey = dayKey,
                )

            if (events.isEmpty()) {
                return false
            }

            // Events are already ordered by (eventAt, id) from repository
            val lastEvent = events.last()
            return lastEvent.eventType == EventType.COMPLETE
        }

        /**
         * Get completion state for all steps in a routine
         * @return Map of stepId to isComplete
         */
        suspend fun getStepStates(
            childId: String,
            routineId: String,
            stepIds: List<String>,
            dayKey: String,
        ): Map<String, Boolean> {
            val allEvents = completionEventRepository.getByChildAndDay(childId, dayKey)
            val routineEvents = allEvents.filter { it.routineId == routineId }

            return stepIds.associateWith { stepId ->
                val stepEvents = routineEvents.filter { it.stepId == stepId }
                if (stepEvents.isEmpty()) {
                    false
                } else {
                    val sortedEvents = stepEvents.sortedWith(CompletionEvent.COMPARATOR)
                    sortedEvents.last().eventType == EventType.COMPLETE
                }
            }
        }
    }
