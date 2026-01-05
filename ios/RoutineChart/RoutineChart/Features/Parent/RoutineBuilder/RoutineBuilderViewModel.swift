import Foundation
import Combine
import SwiftUI
import OSLog

@MainActor
final class RoutineBuilderViewModel: ObservableObject {
    @Published var title: String = ""
    @Published var iconName: String = "📋"
    @Published var steps: [StepInput] = []
    @Published var selectedChildIds: Set<String> = []
    @Published var children: [ChildProfile] = []
    @Published var isSaving = false
    @Published var errorMessage: String?
    
    private let createRoutineUseCase: CreateRoutineUseCase
    private let routineRepository: RoutineRepository
    private let routineStepRepository: RoutineStepRepository
    private let routineAssignmentRepository: RoutineAssignmentRepository
    private let childProfileRepository: ChildProfileRepository
    private let familyRepository: FamilyRepository
    
    private var familyId: String?
    private let existingRoutine: Routine?
    
    struct StepInput: Identifiable {
        let id = UUID()
        var label: String
        var iconName: String
    }
    
    init(
        routine: Routine?,
        createRoutineUseCase: CreateRoutineUseCase,
        routineRepository: RoutineRepository,
        routineStepRepository: RoutineStepRepository,
        routineAssignmentRepository: RoutineAssignmentRepository,
        childProfileRepository: ChildProfileRepository,
        familyRepository: FamilyRepository
    ) {
        self.existingRoutine = routine
        self.createRoutineUseCase = createRoutineUseCase
        self.routineRepository = routineRepository
        self.routineStepRepository = routineStepRepository
        self.routineAssignmentRepository = routineAssignmentRepository
        self.childProfileRepository = childProfileRepository
        self.familyRepository = familyRepository
        
        if let routine = routine {
            self.title = routine.title
            self.iconName = routine.iconName ?? "📋"
        }
    }
    
    func loadData() async {
        do {
            // Get family
            let families = try await familyRepository.getAll()
            guard let family = families.first else {
                errorMessage = "No family found"
                return
            }
            
            familyId = family.id
            
            // Load children
            children = try await childProfileRepository.getAll(familyId: family.id)
            
            // If editing, load existing steps and assignments
            if let routine = existingRoutine {
                let routineSteps = try await routineStepRepository.getAll(routineId: routine.id)
                steps = routineSteps
                    .sorted { $0.orderIndex < $1.orderIndex }
                    .map { StepInput(label: $0.label ?? "", iconName: $0.iconName ?? "⚪️") }
                
                // Load assignments
                let assignments = try await routineAssignmentRepository.getByRoutine(familyId: family.id, routineId: routine.id)
                selectedChildIds = Set(assignments.filter { $0.isActive }.map { $0.childId })
            }
            
            AppLogger.ui.info("Loaded \(self.children.count) children for routine builder")
        } catch {
            AppLogger.ui.error("Error loading routine builder data: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func addStep() {
        steps.append(StepInput(label: "", iconName: "⚪️"))
    }
    
    func removeStep(at index: Int) {
        steps.remove(at: index)
    }
    
    func moveStep(from source: IndexSet, to destination: Int) {
        steps.move(fromOffsets: source, toOffset: destination)
    }
    
    func canSave() -> Bool {
        !title.isEmpty && !steps.isEmpty && steps.allSatisfy { !$0.label.isEmpty }
    }
    
    func save() async -> Bool {
        guard let familyId = familyId else {
            errorMessage = "No family found"
            return false
        }
        
        guard canSave() else {
            errorMessage = "Please fill in all fields"
            return false
        }
        
        isSaving = true
        errorMessage = nil
        
        do {
            let routine: Routine
            
            if let existingRoutine = existingRoutine {
                // Update existing routine
                var updated = existingRoutine
                updated.title = title
                updated.iconName = iconName
                updated.updatedAt = Date()
                
                try await routineRepository.update(updated)
                
                // Delete old steps and create new ones
                let oldSteps = try await routineStepRepository.getAll(routineId: existingRoutine.id)
                for step in oldSteps {
                    try await routineStepRepository.softDelete(id: step.id)
                }
                
                // Create new steps
                for (index, stepInput) in steps.enumerated() {
                    let step = RoutineStep(
                        id: UUID().uuidString,
                        routineId: existingRoutine.id,
                        familyId: familyId,
                        orderIndex: index,
                        label: stepInput.label,
                        iconName: stepInput.iconName,
                        audioCueUrl: nil,
                        createdAt: Date(),
                        deletedAt: nil
                    )
                    try await routineStepRepository.create(step)
                }
                
                routine = updated
            } else {
                // Create new routine with steps
                let stepInputs = steps.map { (label: $0.label, iconName: $0.iconName) }
                routine = try await createRoutineUseCase.execute(
                    familyId: familyId,
                    title: title,
                    iconName: iconName,
                    steps: stepInputs
                )
            }
            
            // Update assignments
            let existingAssignments = try await routineAssignmentRepository.getByRoutine(familyId: familyId, routineId: routine.id)
            
            // Deactivate all existing
            for assignment in existingAssignments {
                var updated = assignment
                updated.isActive = false
                try await routineAssignmentRepository.update(updated)
            }
            
            // Create/activate assignments for selected children
            for childId in selectedChildIds {
                if let existing = existingAssignments.first(where: { $0.childId == childId }) {
                    var updated = existing
                    updated.isActive = true
                    try await routineAssignmentRepository.update(updated)
                } else {
                    let assignment = RoutineAssignment(
                        id: UUID().uuidString,
                        familyId: familyId,
                        routineId: routine.id,
                        childId: childId,
                        isActive: true,
                        assignedAt: Date(),
                        deletedAt: nil
                    )
                    try await routineAssignmentRepository.create(assignment)
                }
            }
            
            AppLogger.ui.info("Saved routine: \(routine.title) with \(self.steps.count) steps")
            isSaving = false
            return true
        } catch {
            AppLogger.ui.error("Error saving routine: \(error.localizedDescription)")
            errorMessage = "Failed to save routine: \(error.localizedDescription)"
            isSaving = false
            return false
        }
    }
}

