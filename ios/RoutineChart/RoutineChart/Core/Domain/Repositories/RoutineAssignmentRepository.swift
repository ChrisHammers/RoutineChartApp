//
//  RoutineAssignmentRepository.swift
//  RoutineChart
//
//  Repository protocol for RoutineAssignment data access
//

import Foundation

protocol RoutineAssignmentRepository {
    func create(_ assignment: RoutineAssignment) async throws
    func get(id: String) async throws -> RoutineAssignment?
    func update(_ assignment: RoutineAssignment) async throws
    func getAll(familyId: String) async throws -> [RoutineAssignment]
    func getByChild(familyId: String, childId: String) async throws -> [RoutineAssignment]
    func getByRoutine(familyId: String, routineId: String) async throws -> [RoutineAssignment]
    func softDelete(id: String) async throws
}

