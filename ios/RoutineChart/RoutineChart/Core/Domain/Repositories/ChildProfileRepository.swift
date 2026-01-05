//
//  ChildProfileRepository.swift
//  RoutineChart
//
//  Repository protocol for ChildProfile data access
//

import Foundation

protocol ChildProfileRepository {
    func create(_ childProfile: ChildProfile) async throws
    func get(id: String) async throws -> ChildProfile?
    func update(_ childProfile: ChildProfile) async throws
    func getAll(familyId: String) async throws -> [ChildProfile]
}

