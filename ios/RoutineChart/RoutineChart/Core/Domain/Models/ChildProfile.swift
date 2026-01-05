//
//  ChildProfile.swift
//  RoutineChart
//
//  Domain model for ChildProfile entity
//

import Foundation

struct ChildProfile: Identifiable, Codable, Equatable, Hashable {
    let id: String
    let familyId: String
    var displayName: String
    var avatarIcon: String?
    var ageBand: AgeBand
    var readingMode: ReadingMode
    var audioEnabled: Bool
    let createdAt: Date
    
    init(
        id: String = UUID().uuidString,
        familyId: String,
        displayName: String,
        avatarIcon: String? = nil,
        ageBand: AgeBand,
        readingMode: ReadingMode,
        audioEnabled: Bool = true,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.familyId = familyId
        self.displayName = displayName
        self.avatarIcon = avatarIcon
        self.ageBand = ageBand
        self.readingMode = readingMode
        self.audioEnabled = audioEnabled
        self.createdAt = createdAt
    }
}

