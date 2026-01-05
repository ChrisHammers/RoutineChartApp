//
//  Family.swift
//  RoutineChart
//
//  Domain model for Family entity
//

import Foundation

struct Family: Identifiable, Codable, Equatable, Hashable {
    let id: String
    var name: String?
    var timeZone: String
    var weekStartsOn: Int
    var planTier: PlanTier
    let createdAt: Date
    var updatedAt: Date
    
    init(
        id: String = UUID().uuidString,
        name: String? = nil,
        timeZone: String = TimeZone.current.identifier,
        weekStartsOn: Int = 0,
        planTier: PlanTier = .free,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.timeZone = timeZone
        self.weekStartsOn = weekStartsOn
        self.planTier = planTier
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

