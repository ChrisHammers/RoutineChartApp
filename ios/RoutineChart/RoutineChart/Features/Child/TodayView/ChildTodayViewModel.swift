//
//  ChildTodayViewModel.swift
//  RoutineChart
//
//  ViewModel for child's today view
//

import Foundation
import Combine
import OSLog

@MainActor
final class ChildTodayViewModel: ObservableObject {
    @Published var children: [ChildProfile] = []
    @Published var selectedChild: ChildProfile?
    @Published var routines: [RoutineWithSteps] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var showSettings = false
    
    let dependencies: AppDependencies
    
    private let childRepo: ChildProfileRepository
    private let routineRepo: RoutineRepository
    private let stepRepo: RoutineStepRepository
    private let assignmentRepo: RoutineAssignmentRepository
    private let eventRepo: CompletionEventRepository
    private let completeStep: CompleteStepUseCase
    private let undoStep: UndoStepUseCase
    private let deriveStepCompletion: DeriveStepCompletionUseCase
    private let authRepository: AuthRepository
    private let userRepository: UserRepository
    
    private var familyId: String?
    private let today: String
    
    init(dependencies: AppDependencies) {
        self.dependencies = dependencies
        self.childRepo = dependencies.childRepo
        self.routineRepo = dependencies.routineRepo
        self.stepRepo = dependencies.stepRepo
        self.assignmentRepo = dependencies.assignmentRepo
        self.eventRepo = dependencies.eventRepo
        self.completeStep = dependencies.completeStep
        self.undoStep = dependencies.undoStep
        self.deriveStepCompletion = dependencies.deriveStepCompletion
        self.authRepository = dependencies.authRepository
        self.userRepository = dependencies.userRepository
        self.today = Date().localDayKey()
    }
    
    func loadData() async {
        isLoading = true
        defer { isLoading = false }
        
        do {
            // Get current authenticated user
            guard let authUser = authRepository.currentUser else {
                error = "Not signed in"
                return
            }
            
            // Get user record to find their familyId
            guard let user = try await userRepository.get(id: authUser.id) else {
                error = "User not found. Please join a family first."
                return
            }
            
            familyId = user.familyId
            
            // Load children for THIS user's family
            children = try await childRepo.getAll(familyId: user.familyId)
            
            // Auto-select first child
            if selectedChild == nil, let first = children.first {
                selectedChild = first
                await loadRoutines()
            }
        } catch {
            self.error = error.localizedDescription
            AppLogger.error("Failed to load data: \(error.localizedDescription)")
        }
    }
    
    func selectChild(_ child: ChildProfile) async {
        selectedChild = child
        await loadRoutines()
    }
    
    func toggleStep(routine: RoutineWithSteps, step: StepWithCompletion) async {
        guard let child = selectedChild, let familyId = familyId else { return }
        
        do {
            if step.isComplete {
                try await undoStep.execute(
                    familyId: familyId,
                    childId: child.id,
                    routineId: routine.routine.id,
                    stepId: step.step.id
                )
            } else {
                try await completeStep.execute(
                    familyId: familyId,
                    childId: child.id,
                    routineId: routine.routine.id,
                    stepId: step.step.id
                )
            }
            
            // Reload to show updated state
            await loadRoutines()
        } catch {
            self.error = error.localizedDescription
            AppLogger.error("Failed to toggle step: \(error.localizedDescription)")
        }
    }
    
    private func loadRoutines() async {
        guard let child = selectedChild, let familyId = familyId else { return }
        
        do {
            // CRITICAL: Pull routines from Firestore BEFORE loading from local
            // This ensures we have the latest data when the UI loads
            if let compositeRepo = routineRepo as? CompositeRoutineRepository,
               let authUser = authRepository.currentUser,
               let user = try? await userRepository.get(id: authUser.id) {
                do {
                    // First, upload any unsynced local changes
                    let uploaded = try await compositeRepo.uploadUnsynced(userId: user.id, familyId: familyId)
                    if uploaded > 0 {
                        AppLogger.ui.info("✅ Uploaded \(uploaded) unsynced routine(s) before loading child view")
                    }
                    
                    // Then, pull any remote changes (this will also pull steps)
                    let pulled = try await compositeRepo.pullRoutines(userId: user.id, familyId: familyId)
                    if pulled > 0 {
                        AppLogger.ui.info("✅ Pulled \(pulled) routine(s) from Firestore before loading child view")
                    }
                } catch {
                    AppLogger.ui.warning("⚠️ Failed to sync routines before loading child view: \(error.localizedDescription)")
                    // Continue loading local data even if sync fails
                }
            }
            
            // Get assignments for this child
            let assignments = try await assignmentRepo.getByChild(familyId: familyId, childId: child.id)
            
            // Get routines and their steps
            var result: [RoutineWithSteps] = []
            for assignment in assignments {
                guard let routine = try await routineRepo.get(id: assignment.routineId) else { continue }
                let steps = try await stepRepo.getAll(routineId: routine.id)
                
                // Get completion state for each step
                var stepsWithCompletion: [StepWithCompletion] = []
                for step in steps {
                    let isComplete = try await deriveStepCompletion.execute(
                        familyId: familyId,
                        childId: child.id,
                        routineId: routine.id,
                        stepId: step.id,
                        localDayKey: today
                    )
                    stepsWithCompletion.append(StepWithCompletion(step: step, isComplete: isComplete))
                }
                
                result.append(RoutineWithSteps(routine: routine, steps: stepsWithCompletion))
            }
            
            routines = result
        } catch {
            self.error = error.localizedDescription
            AppLogger.error("Failed to load routines: \(error.localizedDescription)")
        }
    }
    
    func signOut() {
        do {
            try authRepository.signOut()
            AppLogger.ui.info("User signed out")
        } catch {
            AppLogger.ui.error("Error signing out: \(error.localizedDescription)")
            self.error = "Failed to sign out"
        }
    }
}

// MARK: - View Models

struct RoutineWithSteps: Identifiable {
    let routine: Routine
    let steps: [StepWithCompletion]
    
    var id: String { routine.id }
}

struct StepWithCompletion: Identifiable {
    let step: RoutineStep
    let isComplete: Bool
    
    var id: String { step.id }
}

