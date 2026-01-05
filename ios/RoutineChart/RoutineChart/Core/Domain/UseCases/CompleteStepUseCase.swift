//
//  CompleteStepUseCase.swift
//  RoutineChart
//
//  Use case for completing a step
//

import Foundation
import OSLog

final class CompleteStepUseCase {
    private let eventRepository: CompletionEventRepository
    
    init(eventRepository: CompletionEventRepository) {
        self.eventRepository = eventRepository
    }
    
    func execute(
        familyId: String,
        childId: String,
        routineId: String,
        stepId: String,
        timeZone: TimeZone = .current
    ) async throws {
        let eventId = ULIDGenerator.generate()
        let now = Date()
        let dayKey = now.localDayKey(timeZone: timeZone)
        
        let event = CompletionEvent(
            id: eventId,
            familyId: familyId,
            childId: childId,
            routineId: routineId,
            stepId: stepId,
            eventType: .complete,
            eventAt: now,
            localDayKey: dayKey,
            deviceId: DeviceIdentifier.get(),
            synced: false
        )
        
        try await eventRepository.create(event)
        
        AppLogger.domain.info("Completed step: \(stepId) for child: \(childId)")
    }
}

