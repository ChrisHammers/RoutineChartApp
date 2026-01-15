//
//  AppDependencies.swift
//  RoutineChart
//
//  Dependency injection container
//

import Foundation
import Combine
import OSLog

@MainActor
final class AppDependencies: ObservableObject {
    // Auth
    let authRepo: AuthRepository
    @Published var currentAuthUser: AuthUser?
    @Published var currentUser: User?
    
    // Repositories
    let familyRepo: FamilyRepository
    let userRepo: UserRepository
    let childRepo: ChildProfileRepository
    let routineRepo: RoutineRepository
    let stepRepo: RoutineStepRepository
    let assignmentRepo: RoutineAssignmentRepository
    let eventRepo: CompletionEventRepository
    let inviteRepo: FamilyInviteRepository
    
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
    var userRepository: UserRepository { userRepo }
    var childProfileRepository: ChildProfileRepository { childRepo }
    var routineRepository: RoutineRepository { routineRepo }
    var routineStepRepository: RoutineStepRepository { stepRepo }
    var routineAssignmentRepository: RoutineAssignmentRepository { assignmentRepo }
    var completionEventRepository: CompletionEventRepository { eventRepo }
    var familyInviteRepository: FamilyInviteRepository { inviteRepo }
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
        // Use composite repository for families (SQLite + Firestore sync)
        self.familyRepo = CompositeFamilyRepository()
        // Use composite repository for users (SQLite + Firestore sync)
        self.userRepo = CompositeUserRepository()
        self.childRepo = SQLiteChildProfileRepository()
        self.routineRepo = SQLiteRoutineRepository()
        self.stepRepo = SQLiteRoutineStepRepository()
        self.assignmentRepo = SQLiteRoutineAssignmentRepository()
        self.eventRepo = SQLiteCompletionEventRepository()
        // Use composite repository for invites (SQLite + Firestore sync)
        self.inviteRepo = CompositeFamilyInviteRepository()
        
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
            .sink { [weak self] authUser in
                self?.currentAuthUser = authUser
                Task { @MainActor in
                    await self?.loadCurrentUser()
                }
            }
            .store(in: &cancellables)
    }
    
    func loadCurrentUser() async {
        guard let authUser = currentAuthUser else {
            currentUser = nil
            return
        }
        
        do {
            // Try to load existing user
            if let user = try await userRepo.get(id: authUser.id) {
                currentUser = user
                return
            }
            
            // No User record exists - create one
            // If anonymous, don't create (they join via invite)
            // If non-anonymous (email/password), assume parent and create family + user
            if authUser.isAnonymous {
                // Anonymous users join families via invite, so don't create User record yet
                currentUser = nil
                return
            }
            
            // Non-anonymous user (parent) - create Family and User record
            let family = Family(
                id: ULIDGenerator.generate(),
                name: nil,
                timeZone: TimeZone.current.identifier,
                weekStartsOn: 0,
                planTier: .free
            )
            try await familyRepo.create(family)
            
            let newUser = User(
                id: authUser.id,
                familyId: family.id,
                role: .parent,
                displayName: authUser.email?.components(separatedBy: "@").first ?? "Parent",
                email: authUser.email,
                createdAt: Date()
            )
            try await userRepo.create(newUser)
            currentUser = newUser
            
            AppLogger.database.info("Created family and user for parent: \(authUser.id)")
        } catch {
            AppLogger.error("Failed to load/create current user: \(error.localizedDescription)")
            currentUser = nil
        }
    }
}

