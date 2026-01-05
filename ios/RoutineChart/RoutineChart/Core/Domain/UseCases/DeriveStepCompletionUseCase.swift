//
//  DeriveStepCompletionUseCase.swift
//  RoutineChart
//
//  Use case for deriving step completion state from event log
//

import Foundation

final class DeriveStepCompletionUseCase {
    private let eventRepository: CompletionEventRepository
    
    init(eventRepository: CompletionEventRepository) {
        self.eventRepository = eventRepository
    }
    
    func execute(
        familyId: String,
        childId: String,
        routineId: String,
        stepId: String,
        localDayKey: String
    ) async throws -> Bool {
        // Get all events for this step on this day
        let events = try await eventRepository.getEvents(
            familyId: familyId,
            childId: childId,
            routineId: routineId,
            stepId: stepId,
            localDayKey: localDayKey
        )
        
        // Events are already sorted by eventAt, then eventId
        // Last event type determines current state
        guard let lastEvent = events.last else {
            return false
        }
        
        return lastEvent.eventType == .complete
    }
}

