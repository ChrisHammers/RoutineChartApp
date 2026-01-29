//
//  SeedDataManager.swift
//  RoutineChart
//
//  Seed data for testing Phase 1
//

import Foundation
import OSLog

final class SeedDataManager {
    private let familyRepo: FamilyRepository
    private let childRepo: ChildProfileRepository
    private let routineRepo: RoutineRepository
    private let stepRepo: RoutineStepRepository
    private let assignmentRepo: RoutineAssignmentRepository
    private let userRepo: UserRepository
    
    init(
        familyRepo: FamilyRepository,
        childRepo: ChildProfileRepository,
        routineRepo: RoutineRepository,
        stepRepo: RoutineStepRepository,
        assignmentRepo: RoutineAssignmentRepository,
        userRepo: UserRepository
    ) {
        self.familyRepo = familyRepo
        self.childRepo = childRepo
        self.routineRepo = routineRepo
        self.stepRepo = stepRepo
        self.assignmentRepo = assignmentRepo
        self.userRepo = userRepo
    }
    
    /// Seeds demo routines (Morning, Bedtime) only when the user has no routines.
    /// Call after sync so local DB may already have routines from Firestore.
    /// When familyId is provided and the family exists, seeds into that family so new routines sync to the cloud.
    func seedIfNeeded(userId: String, familyId: String? = nil) async throws {
        // Only seed when user has no routines (e.g. fresh start)
        let existingRoutines = try await routineRepo.getAll(userId: userId, familyId: nil, includeDeleted: false)
        guard existingRoutines.isEmpty else {
            AppLogger.database.info("[Seed] User already has \(existingRoutines.count) routine(s), skipping seed")
            return
        }
        
        AppLogger.database.info("[Seed] No routines found, seeding initial data...")
        
        let family: Family
        let child1: ChildProfile
        let child2: ChildProfile
        
        if let fid = familyId, let existingFamily = try await familyRepo.get(id: fid) {
            family = existingFamily
            let existingChildren = try await childRepo.getAll(familyId: fid)
            if existingChildren.isEmpty {
                child1 = ChildProfile(
                    familyId: family.id,
                    displayName: "Emma",
                    avatarIcon: "🌟",
                    ageBand: .age_5_7,
                    readingMode: .light_text
                )
                try await childRepo.create(child1)
                child2 = ChildProfile(
                    familyId: family.id,
                    displayName: "Noah",
                    avatarIcon: "🚀",
                    ageBand: .age_8_10,
                    readingMode: .full_text
                )
                try await childRepo.create(child2)
            } else {
                child1 = existingChildren[0]
                child2 = existingChildren.count > 1 ? existingChildren[1] : ChildProfile(
                    familyId: family.id,
                    displayName: "Noah",
                    avatarIcon: "🚀",
                    ageBand: .age_8_10,
                    readingMode: .full_text
                )
                if existingChildren.count < 2 {
                    try await childRepo.create(child2)
                }
            }
        } else {
            // Create new family and children
            family = Family(
                name: "Test Family",
                timeZone: TimeZone.current.identifier,
                weekStartsOn: 0,
                planTier: .free
            )
            try await familyRepo.create(family)
            
            child1 = ChildProfile(
                familyId: family.id,
                displayName: "Emma",
                avatarIcon: "🌟",
                ageBand: .age_5_7,
                readingMode: .light_text
            )
            try await childRepo.create(child1)
            
            child2 = ChildProfile(
                familyId: family.id,
                displayName: "Noah",
                avatarIcon: "🚀",
                ageBand: .age_8_10,
                readingMode: .full_text
            )
            try await childRepo.create(child2)
        }
        
        // Create Morning Routine
        let morningRoutine = Routine(
            userId: userId,
            familyId: family.id,
            title: "Morning Routine",
            iconName: "☀️"
        )
        try await routineRepo.create(morningRoutine)
        
        let morningSteps: [(String, String)] = [
            ("Wake up", "🛏️"),
            ("Brush teeth", "🪥"),
            ("Get dressed", "👕"),
            ("Eat breakfast", "🥞"),
            ("Pack backpack", "🎒")
        ]
        
        for (index, (label, icon)) in morningSteps.enumerated() {
            let step = RoutineStep(
                routineId: morningRoutine.id,
                orderIndex: index,
                label: label,
                iconName: icon
            )
            try await stepRepo.create(step)
        }
        
        // Create Bedtime Routine
        let bedtimeRoutine = Routine(
            userId: userId,
            familyId: family.id,
            title: "Bedtime Routine",
            iconName: "🌙"
        )
        try await routineRepo.create(bedtimeRoutine)
        
        let bedtimeSteps: [(String, String)] = [
            ("Put on pajamas", "🩲"),
            ("Brush teeth", "🪥"),
            ("Read a book", "📚"),
            ("Say goodnight", "🤗"),
            ("Lights out", "💤")
        ]
        
        for (index, (label, icon)) in bedtimeSteps.enumerated() {
            let step = RoutineStep(
                routineId: bedtimeRoutine.id,
                orderIndex: index,
                label: label,
                iconName: icon
            )
            try await stepRepo.create(step)
        }
        
        // Assign routines to children
        let assignment1 = RoutineAssignment(
            familyId: family.id,
            routineId: morningRoutine.id,
            childId: child1.id
        )
        try await assignmentRepo.create(assignment1)
        
        let assignment2 = RoutineAssignment(
            familyId: family.id,
            routineId: morningRoutine.id,
            childId: child2.id
        )
        try await assignmentRepo.create(assignment2)
        
        let assignment3 = RoutineAssignment(
            familyId: family.id,
            routineId: bedtimeRoutine.id,
            childId: child1.id
        )
        try await assignmentRepo.create(assignment3)
        
        let assignment4 = RoutineAssignment(
            familyId: family.id,
            routineId: bedtimeRoutine.id,
            childId: child2.id
        )
        try await assignmentRepo.create(assignment4)
        
        AppLogger.database.info("[Seed] Seed data created: 1 family, 2 children, 2 routines, 10 steps, 4 assignments")
    }
}

