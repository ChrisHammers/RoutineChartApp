//
//  SeedDataManager.swift
//  RoutineChart
//
//  Seeds initial routines (Morning, Bedtime) only when the user has no routines.
//  Does not add children; parent can add children and assign routines later.
//

import Foundation
import OSLog

final class SeedDataManager {
    private let familyRepo: FamilyRepository
    private let routineRepo: RoutineRepository
    private let stepRepo: RoutineStepRepository
    private let userRepo: UserRepository
    
    init(
        familyRepo: FamilyRepository,
        routineRepo: RoutineRepository,
        stepRepo: RoutineStepRepository,
        userRepo: UserRepository
    ) {
        self.familyRepo = familyRepo
        self.routineRepo = routineRepo
        self.stepRepo = stepRepo
        self.userRepo = userRepo
    }
    
    /// Seeds demo routines (Morning, Bedtime) only when the user has no routines.
    /// Call after sync so local DB may already have routines from Firestore.
    /// When familyId is provided and the family exists, seeds into that family so new routines sync to the cloud.
    /// Does not create children or assignments.
    func seedIfNeeded(userId: String, familyId: String? = nil) async throws {
        let existingRoutines = try await routineRepo.getAll(userId: userId, familyId: nil, includeDeleted: false)
        guard existingRoutines.isEmpty else {
            AppLogger.database.info("[Seed] User already has \(existingRoutines.count) routine(s), skipping seed")
            return
        }
        
        AppLogger.database.info("[Seed] No routines found, seeding initial data (routines only, no children)...")
        
        let family: Family
        if let fid = familyId, let existingFamily = try await familyRepo.get(id: fid) {
            family = existingFamily
        } else {
            family = Family(
                name: "Test Family",
                timeZone: TimeZone.current.identifier,
                weekStartsOn: 0,
                planTier: .free
            )
            try await familyRepo.create(family)
        }
        
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
        
        AppLogger.database.info("[Seed] Seed data created: 1 family, 2 routines, 10 steps (no children)")
    }
}
