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
    // Auth
    let authRepo: AuthRepository
    @Published var currentAuthUser: AuthUser?
    
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
    
    // Combine cancellables
    private var cancellables = Set<AnyCancellable>()
    
    // Convenience accessors for ViewModels (full names)
    var authRepository: AuthRepository { authRepo }
    var familyRepository: FamilyRepository { familyRepo }
    var childProfileRepository: ChildProfileRepository { childRepo }
    var routineRepository: RoutineRepository { routineRepo }
    var routineStepRepository: RoutineStepRepository { stepRepo }
    var routineAssignmentRepository: RoutineAssignmentRepository { assignmentRepo }
    var completionEventRepository: CompletionEventRepository { eventRepo }
    var createRoutineUseCase: CreateRoutineUseCase { createRoutine }
    var completeStepUseCase: CompleteStepUseCase { completeStep }
    var undoStepUseCase: UndoStepUseCase { undoStep }
    var deriveStepCompletionUseCase: DeriveStepCompletionUseCase { deriveStepCompletion }
    var deriveRoutineCompletionUseCase: DeriveRoutineCompletionUseCase { deriveRoutineCompletion }
    
    init() {
        // Initialize auth repository
        self.authRepo = FirebaseAuthService()
        self.currentAuthUser = authRepo.currentUser
        
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
        
        // Subscribe to auth state changes
        authRepo.authStatePublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$currentAuthUser)
    }
}

