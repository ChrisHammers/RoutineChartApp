//
//  CompletionEvent.swift
//  RoutineChart
//
//  Domain model for CompletionEvent entity (append-only event log)
//

import Foundation

struct CompletionEvent: Identifiable, Codable, Equatable, Hashable {
    let id: String // ULID
    let familyId: String
    let childId: String
    let routineId: String
    let stepId: String
    let eventType: EventType
    let eventAt: Date
    let localDayKey: String // YYYY-MM-DD
    let deviceId: String
    
    // Local-only property for sync tracking (not part of domain model)
    var synced: Bool = false
    
    init(
        id: String,
        familyId: String,
        childId: String,
        routineId: String,
        stepId: String,
        eventType: EventType,
        eventAt: Date,
        localDayKey: String,
        deviceId: String,
        synced: Bool = false
    ) {
        self.id = id
        self.familyId = familyId
        self.childId = childId
        self.routineId = routineId
        self.stepId = stepId
        self.eventType = eventType
        self.eventAt = eventAt
        self.localDayKey = localDayKey
        self.deviceId = deviceId
        self.synced = synced
    }
}

