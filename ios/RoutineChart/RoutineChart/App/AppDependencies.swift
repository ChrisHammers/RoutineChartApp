//
//  AppDependencies.swift
//  RoutineChart
//
//  Dependency injection container
//

import Foundation
import Combine

@MainActor
final class AppDependencies: ObservableObject {
    // Repositories
    let familyRepo: FamilyRepository
    let childRepo: ChildProfileRepository
    let routineRepo: RoutineRepository
    let stepRepo: RoutineStepRepository
    let assignmentRepo: RoutineAssignmentRepository
    let eventRepo: CompletionEventRepository
    
    // Use Cases
    let createRoutine: CreateRoutineUseCase
    let completeStep: CompleteStepUseCase
    let undoStep: UndoStepUseCase
    let deriveStepCompletion: DeriveStepCompletionUseCase
    let deriveRoutineCompletion: DeriveRoutineCompletionUseCase
    
    // Seed Data
    let seedDataManager: SeedDataManager
    
    init() {
        // Initialize repositories
        self.familyRepo = SQLiteFamilyRepository()
        self.childRepo = SQLiteChildProfileRepository()
        self.routineRepo = SQLiteRoutineRepository()
        self.stepRepo = SQLiteRoutineStepRepository()
        self.assignmentRepo = SQLiteRoutineAssignmentRepository()
        self.eventRepo = SQLiteCompletionEventRepository()
        
        // Initialize use cases
        self.createRoutine = CreateRoutineUseCase(
            routineRepository: routineRepo,
            stepRepository: stepRepo
        )
        
        self.completeStep = CompleteStepUseCase(
            eventRepository: eventRepo
        )
        
        self.undoStep = UndoStepUseCase(
            eventRepository: eventRepo
        )
        
        self.deriveStepCompletion = DeriveStepCompletionUseCase(
            eventRepository: eventRepo
        )
        
        self.deriveRoutineCompletion = DeriveRoutineCompletionUseCase(
            stepRepository: stepRepo,
            deriveStepCompletion: deriveStepCompletion
        )
        
        // Initialize seed data manager
        self.seedDataManager = SeedDataManager(
            familyRepo: familyRepo,
            childRepo: childRepo,
            routineRepo: routineRepo,
            stepRepo: stepRepo,
            assignmentRepo: assignmentRepo
        )
    }
}

