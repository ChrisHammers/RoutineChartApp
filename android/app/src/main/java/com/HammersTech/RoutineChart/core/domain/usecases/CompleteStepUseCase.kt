package com.HammersTech.RoutineChart.core.domain.usecases

import com.HammersTech.RoutineChart.core.domain.models.CompletionEvent
import com.HammersTech.RoutineChart.core.domain.models.EventType
import com.HammersTech.RoutineChart.core.domain.repositories.CompletionEventRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.HammersTech.RoutineChart.core.utils.DateHelpers
import com.HammersTech.RoutineChart.core.utils.ULIDGenerator
import java.time.Instant
import javax.inject.Inject

/**
 * Use case for recording a step completion event
 */
class CompleteStepUseCase @Inject constructor(
    private val completionEventRepository: CompletionEventRepository
) {
    suspend operator fun invoke(
        familyId: String,
        familyTimeZone: String,
        childId: String,
        routineId: String,
        stepId: String,
        deviceId: String
    ): CompletionEvent {
        val now = Instant.now()
        val dayKey = DateHelpers.localDayKey(now, familyTimeZone)

        val event = CompletionEvent(
            id = ULIDGenerator.generate(),
            familyId = familyId,
            childId = childId,
            routineId = routineId,
            stepId = stepId,
            eventType = EventType.COMPLETE,
            eventAt = now,
            localDayKey = dayKey,
            deviceId = deviceId,
            synced = false
        )

        completionEventRepository.create(event)
        AppLogger.UseCase.info("Completed step: $stepId for child: $childId")
        return event
    }
}

