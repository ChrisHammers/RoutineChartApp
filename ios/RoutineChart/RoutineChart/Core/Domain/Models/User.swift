//
//  User.swift
//  RoutineChart
//
//  Domain model for User entity
//

import Foundation
import Combine

struct User: Identifiable, Codable, Equatable, Hashable {
    let id: String
    let familyId: String
    var role: Role
    var displayName: String
    var email: String?
    let createdAt: Date
    
    init(
        id: String = UUID().uuidString,
        familyId: String,
        role: Role,
        displayName: String,
        email: String? = nil,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.familyId = familyId
        self.role = role
        self.displayName = displayName
        self.email = email
        self.createdAt = createdAt
    }
}

