//
//  RoutineStep.swift
//  RoutineChart
//
//  Domain model for RoutineStep entity
//

import Foundation

struct RoutineStep: Identifiable, Codable, Equatable, Hashable, Sendable {
    let id: String
    let routineId: String
    var orderIndex: Int
    var label: String?
    var iconName: String?
    var audioCueUrl: String?
    let createdAt: Date
    var deletedAt: Date?
    
    var isDeleted: Bool {
        deletedAt != nil
    }
    
    init(
        id: String = UUID().uuidString,
        routineId: String,
        orderIndex: Int,
        label: String? = nil,
        iconName: String? = nil,
        audioCueUrl: String? = nil,
        createdAt: Date = Date(),
        deletedAt: Date? = nil
    ) {
        self.id = id
        self.routineId = routineId
        self.orderIndex = orderIndex
        self.label = label
        self.iconName = iconName
        self.audioCueUrl = audioCueUrl
        self.createdAt = createdAt
        self.deletedAt = deletedAt
    }
}

