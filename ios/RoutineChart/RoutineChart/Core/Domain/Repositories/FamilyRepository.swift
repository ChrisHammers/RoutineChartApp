//
//  FamilyRepository.swift
//  RoutineChart
//
//  Repository protocol for Family data access
//

import Foundation

protocol FamilyRepository {
    func create(_ family: Family) async throws
    func get(id: String) async throws -> Family?
    func update(_ family: Family) async throws
    func getAll() async throws -> [Family]
}

