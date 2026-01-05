//
//  ChildTodayViewModel.swift
//  RoutineChart
//
//  ViewModel for child's today view
//

import Foundation
import Combine
import GRDB

@MainActor
final class ChildTodayViewModel: ObservableObject {
    @Published var children: [ChildProfile] = []
    @Published var selectedChild: ChildProfile?
    @Published var routines: [RoutineWithSteps] = []
    @Published var isLoading = false
    @Published var error: String?
    
    private let childRepo: ChildProfileRepository
    private let routineRepo: RoutineRepository
    private let stepRepo: RoutineStepRepository
    private let assignmentRepo: RoutineAssignmentRepository
    private let eventRepo: CompletionEventRepository
    private let completeStep: CompleteStepUseCase
    private let undoStep: UndoStepUseCase
    private let deriveStepCompletion: DeriveStepCompletionUseCase
    
    private var familyId: String?
    private let today: String
    
    init(dependencies: AppDependencies) {
        self.childRepo = dependencies.childRepo
        self.routineRepo = dependencies.routineRepo
        self.stepRepo = dependencies.stepRepo
        self.assignmentRepo = dependencies.assignmentRepo
        self.eventRepo = dependencies.eventRepo
        self.completeStep = dependencies.completeStep
        self.undoStep = dependencies.undoStep
        self.deriveStepCompletion = dependencies.deriveStepCompletion
        self.today = Date().localDayKey()
    }
    
    func loadData() async {
        isLoading = true
        defer { isLoading = false }
        
        do {
            // Get family (Phase 1: using first family)
            let families = try await childRepo.getAll(familyId: "") // Hack: get all
            guard let family = try await getFirstFamily() else {
                error = "No family found"
                return
            }
            familyId = family.id
            
            // Load children
            children = try await childRepo.getAll(familyId: family.id)
            
            // Auto-select first child
            if selectedChild == nil, let first = children.first {
                selectedChild = first
                await loadRoutines()
            }
        } catch {
            self.error = error.localizedDescription
            AppLogger.error("Failed to load data", error: error)
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
            AppLogger.error("Failed to toggle step", error: error)
        }
    }
    
    private func loadRoutines() async {
        guard let child = selectedChild, let familyId = familyId else { return }
        
        do {
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
            AppLogger.error("Failed to load routines", error: error)
        }
    }
    
    private func getFirstFamily() async throws -> Family? {
        // Phase 1 hack: get first family from database
        let db = try SQLiteManager.shared.database()
        return try await db.read { db in
            try Family.fetchOne(db)
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

