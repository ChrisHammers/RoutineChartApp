//
//  RoutineStepRepository.swift
//  RoutineChart
//
//  Repository protocol for RoutineStep data access
//

import Foundation

protocol RoutineStepRepository {
    func create(_ step: RoutineStep) async throws
    func get(id: String) async throws -> RoutineStep?
    func update(_ step: RoutineStep) async throws
    func getAll(routineId: String) async throws -> [RoutineStep]
    func softDelete(id: String) async throws
}

