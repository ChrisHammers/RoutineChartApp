//
//  DeriveRoutineCompletionUseCase.swift
//  RoutineChart
//
//  Use case for deriving routine completion state
//

import Foundation

final class DeriveRoutineCompletionUseCase {
    private let stepRepository: RoutineStepRepository
    private let deriveStepCompletion: DeriveStepCompletionUseCase
    
    init(
        stepRepository: RoutineStepRepository,
        deriveStepCompletion: DeriveStepCompletionUseCase
    ) {
        self.stepRepository = stepRepository
        self.deriveStepCompletion = deriveStepCompletion
    }
    
    func execute(
        familyId: String,
        childId: String,
        routineId: String,
        localDayKey: String
    ) async throws -> Bool {
        // Get all steps for this routine
        let steps = try await stepRepository.getAll(routineId: routineId)
        
        // Routine is complete if ALL steps are complete
        for step in steps {
            let isStepComplete = try await deriveStepCompletion.execute(
                familyId: familyId,
                childId: childId,
                routineId: routineId,
                stepId: step.id,
                localDayKey: localDayKey
            )
            
            if !isStepComplete {
                return false
            }
        }
        
        return !steps.isEmpty // Empty routine is not complete
    }
}

