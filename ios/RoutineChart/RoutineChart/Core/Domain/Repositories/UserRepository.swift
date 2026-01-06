//
//  UserRepository.swift
//  RoutineChart
//
//  Repository protocol for User data access
//

import Foundation

protocol UserRepository {
    func create(_ user: User) async throws
    func get(id: String) async throws -> User?
    func update(_ user: User) async throws
    func getAll(familyId: String) async throws -> [User]
    
    /// Update user's family ID (for joining a family)
    func updateFamilyId(userId: String, familyId: String) async throws
}

