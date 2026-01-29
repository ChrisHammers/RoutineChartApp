//
//  RoutineAssignment.swift
//  RoutineChart
//
//  Domain model for RoutineAssignment entity
//

import Foundation
import Combine

struct RoutineAssignment: Identifiable, Codable, Equatable, Hashable {
    let id: String
    let familyId: String
    let routineId: String
    let childId: String
    var isActive: Bool
    let assignedAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    
    var isDeleted: Bool {
        deletedAt != nil
    }
    
    init(
        id: String = UUID().uuidString,
        familyId: String,
        routineId: String,
        childId: String,
        isActive: Bool = true,
        assignedAt: Date = Date(),
        updatedAt: Date? = nil,
        deletedAt: Date? = nil
    ) {
        self.id = id
        self.familyId = familyId
        self.routineId = routineId
        self.childId = childId
        self.isActive = isActive
        self.assignedAt = assignedAt
        self.updatedAt = updatedAt ?? assignedAt
        self.deletedAt = deletedAt
    }
}

