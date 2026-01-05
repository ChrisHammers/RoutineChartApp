//
//  CreateRoutineUseCase.swift
//  RoutineChart
//
//  Use case for creating a routine with steps
//

import Foundation
import OSLog

final class CreateRoutineUseCase {
    private let routineRepository: RoutineRepository
    private let stepRepository: RoutineStepRepository
    
    init(
        routineRepository: RoutineRepository,
        stepRepository: RoutineStepRepository
    ) {
        self.routineRepository = routineRepository
        self.stepRepository = stepRepository
    }
    
    func execute(
        familyId: String,
        title: String,
        iconName: String? = nil,
        steps: [(label: String?, iconName: String?)]
    ) async throws -> Routine {
        // Create routine
        let routine = Routine(
            familyId: familyId,
            title: title,
            iconName: iconName
        )
        
        try await routineRepository.create(routine)
        
        // Create steps
        for (index, stepData) in steps.enumerated() {
            let step = RoutineStep(
                routineId: routine.id,
                familyId: familyId,
                orderIndex: index,
                label: stepData.label,
                iconName: stepData.iconName
            )
            try await stepRepository.create(step)
        }
        
        AppLogger.domain.info("Created routine '\(title)' with \(steps.count) steps")
        
        return routine
    }
}

