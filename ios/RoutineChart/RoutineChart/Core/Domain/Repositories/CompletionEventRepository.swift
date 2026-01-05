//
//  CompletionEventRepository.swift
//  RoutineChart
//
//  Repository protocol for CompletionEvent data access
//

import Foundation

protocol CompletionEventRepository {
    func create(_ event: CompletionEvent) async throws
    func getEvents(
        familyId: String,
        childId: String?,
        routineId: String?,
        stepId: String?,
        localDayKey: String?
    ) async throws -> [CompletionEvent]
    func getUnsyncedEvents() async throws -> [CompletionEvent]
    func markSynced(eventId: String) async throws
}

