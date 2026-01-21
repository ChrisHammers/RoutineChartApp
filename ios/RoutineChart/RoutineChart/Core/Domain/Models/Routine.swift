//
//  Routine.swift
//  RoutineChart
//
//  Domain model for Routine entity
//

import Foundation

struct Routine: Identifiable, Codable, Equatable, Hashable, Sendable {
    let id: String
    let userId: String // Owner of the routine (required)
    var familyId: String? // Optional - if nil, routine is personal; if present, routine is shared with family
    var title: String
    var iconName: String?
    var version: Int
    var completionRule: CompletionRule
    let createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?
    
    var isDeleted: Bool {
        deletedAt != nil
    }
    
    init(
        id: String = UUID().uuidString,
        userId: String,
        familyId: String? = nil,
        title: String,
        iconName: String? = nil,
        version: Int = 1,
        completionRule: CompletionRule = .all_steps_required,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        deletedAt: Date? = nil
    ) {
        self.id = id
        self.userId = userId
        self.familyId = familyId
        self.title = title
        self.iconName = iconName
        self.version = version
        self.completionRule = completionRule
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
    }
}

