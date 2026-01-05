//
//  RoutineRepository.swift
//  RoutineChart
//
//  Repository protocol for Routine data access
//

import Foundation

protocol RoutineRepository {
    func create(_ routine: Routine) async throws
    func get(id: String) async throws -> Routine?
    func update(_ routine: Routine) async throws
    func getAll(familyId: String, includeDeleted: Bool) async throws -> [Routine]
    func softDelete(id: String) async throws
}

