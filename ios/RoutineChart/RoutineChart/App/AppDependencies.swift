//
//  AppDependencies.swift
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
        // Pass familyRepo so User sync can ensure Family exists before updating User (foreign key constraint)
        self.userRepo = CompositeUserRepository(familyRepo: self.familyRepo)
        self.childRepo = SQLiteChildProfileRepository()
        // Use composite repository for routines (SQLite + Firestore sync via upload queue)
        self.routineRepo = CompositeRoutineRepository()
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
            assignmentRepo: assignmentRepo,
            userRepo: userRepo
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
        
        // Safeguard: If user is already loaded and matches auth user, skip reload
        if let existingUser = currentUser, existingUser.id == authUser.id {
            AppLogger.database.info("✅ User already loaded: \(authUser.id), skipping reload")
            return
        }
        
        do {
            // CRITICAL: Check local User FIRST to prevent duplicate Family creation
            // Only sync from Firestore if User exists locally (to update with Firestore data)
            var existingUser: User? = try await userRepo.get(id: authUser.id)
            
            if let localUser = existingUser {
                // User exists locally - sync from Firestore to update with latest data
                if !authUser.isAnonymous {
                    if let compositeRepo = userRepo as? CompositeUserRepository {
                        do {
                            try await compositeRepo.syncFromFirestore(userId: authUser.id)
                            // Reload from local after sync (local was updated with Firestore data)
                            existingUser = try await userRepo.get(id: authUser.id)
                            if let user = existingUser {
                                AppLogger.database.info("✅ Synced user from Firestore: \(authUser.id), familyId: \(user.familyId)")
                            }
                        } catch {
                            // If Firestore sync fails, use local user
                            AppLogger.database.warning("⚠️ Failed to sync user from Firestore, using local: \(error.localizedDescription)")
                            existingUser = localUser
                        }
                    }
                }
                
                // Use the user (either synced or local)
                currentUser = existingUser
                
                // Phase 3.2: Upload unsynced routines (early implementation of Phase 3.8 background sync)
                // Phase 3.3: Pull routines from Firestore (early implementation of Phase 3.8 background sync)
                // CRITICAL: Pull synchronously to ensure routines are available when UI loads
                if let user = existingUser, let compositeRepo = routineRepo as? CompositeRoutineRepository {
                    do {
                        // First, upload any unsynced local changes
                        let uploaded = try await compositeRepo.uploadUnsynced(familyId: user.familyId)
                        if uploaded > 0 {
                            AppLogger.database.info("✅ Uploaded \(uploaded) unsynced routine(s) on app launch")
                        }
                        
                        // Then, pull any remote changes (this will also pull steps)
                        let pulled = try await compositeRepo.pullRoutines(userId: user.id, familyId: user.familyId)
                        if pulled > 0 {
                            AppLogger.database.info("✅ Pulled \(pulled) routine(s) from Firestore on app launch")
                        }
                    } catch {
                        AppLogger.database.warning("⚠️ Failed to sync routines: \(error.localizedDescription)")
                        // Continue even if sync fails - UI will show local data
                    }
                }
                
                return
            }
            
            // No User record exists locally - check Firestore for non-anonymous users
            if !authUser.isAnonymous {
                // For non-anonymous users, try to sync from Firestore first
                // This handles the case where User exists in Firestore but not locally
                if let compositeRepo = userRepo as? CompositeUserRepository {
                    do {
                        try await compositeRepo.syncFromFirestore(userId: authUser.id)
                        // Reload from local after sync
                        existingUser = try await userRepo.get(id: authUser.id)
                        if let user = existingUser {
                            AppLogger.database.info("✅ Synced user from Firestore: \(authUser.id), familyId: \(user.familyId)")
                            currentUser = user
                            
                            // CRITICAL: Pull routines after syncing user from Firestore
                            // This ensures routines are available when UI loads
                            if let routineCompositeRepo = routineRepo as? CompositeRoutineRepository {
                                do {
                                    // First, upload any unsynced local changes
                                    let uploaded = try await routineCompositeRepo.uploadUnsynced(familyId: user.familyId)
                                    if uploaded > 0 {
                                        AppLogger.database.info("✅ Uploaded \(uploaded) unsynced routine(s) after user sync")
                                    }
                                    
                                    // Then, pull any remote changes
                                    let pulled = try await routineCompositeRepo.pullRoutines(userId: user.id, familyId: user.familyId)
                                    if pulled > 0 {
                                        AppLogger.database.info("✅ Pulled \(pulled) routine(s) from Firestore after user sync")
                                    }
                                } catch {
                                    AppLogger.database.warning("⚠️ Failed to sync routines after user sync: \(error.localizedDescription)")
                                }
                            }
                            
                            return
                        }
                    } catch {
                        // User doesn't exist in Firestore - will create new one below
                        AppLogger.database.info("ℹ️ User not found in Firestore, will create new: \(authUser.id)")
                    }
                }
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
            // CRITICAL: Double-check that User doesn't exist (race condition protection)
            existingUser = try await userRepo.get(id: authUser.id)
            if let user = existingUser {
                // User was created by another concurrent call - use it
                AppLogger.database.info("✅ User was created by concurrent call, using existing: \(authUser.id)")
                currentUser = user
                return
            }
            
            // User definitely doesn't exist - create Family and User
            let family = Family(
                id: ULIDGenerator.generate(),
                name: nil,
                timeZone: TimeZone.current.identifier,
                weekStartsOn: 0,
                planTier: .free
            )
            try await familyRepo.create(family)
            
            // CRITICAL: Check one more time before creating User (race condition protection)
            existingUser = try await userRepo.get(id: authUser.id)
            if let user = existingUser {
                // User was created by another concurrent call after we created Family
                AppLogger.database.warning("⚠️ User was created by concurrent call after Family creation, using existing: \(authUser.id)")
                currentUser = user
                return
            }
            
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
            
            AppLogger.database.info("✅ Created family and user for parent: \(authUser.id), familyId: \(family.id)")
            
            // Phase 3.2: Upload unsynced routines (early implementation of Phase 3.8 background sync)
            // Phase 3.3: Pull routines from Firestore (early implementation of Phase 3.8 background sync)
            // CRITICAL: Pull synchronously to ensure routines are available when UI loads
            if let compositeRepo = routineRepo as? CompositeRoutineRepository {
                do {
                    // First, upload any unsynced local changes
                    let uploaded = try await compositeRepo.uploadUnsynced(familyId: family.id)
                    if uploaded > 0 {
                        AppLogger.database.info("✅ Uploaded \(uploaded) unsynced routine(s) on app launch")
                    }
                    
                    // Then, pull any remote changes (this will also pull steps)
                    let pulled = try await compositeRepo.pullRoutines(userId: newUser.id, familyId: newUser.familyId)
                    if pulled > 0 {
                        AppLogger.database.info("✅ Pulled \(pulled) routine(s) from Firestore on app launch")
                    }
                } catch {
                    AppLogger.database.warning("⚠️ Failed to sync routines: \(error.localizedDescription)")
                    // Continue even if sync fails - UI will show local data
                }
            }
        } catch {
            AppLogger.error("Failed to load/create current user: \(error.localizedDescription)")
            currentUser = nil
        }
    }
}
