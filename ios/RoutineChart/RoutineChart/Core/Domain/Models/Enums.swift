//
//  Enums.swift
//  RoutineChart
//
//  Core domain enums matching specification
//

import Foundation

// MARK: - Role

enum Role: String, Codable, CaseIterable, Hashable {
    case parent
    case child
}

// MARK: - PlanTier

enum PlanTier: String, Codable, CaseIterable, Hashable {
    case free
    case paid
}

// MARK: - AgeBand

enum AgeBand: String, Codable, CaseIterable, Hashable {
    case age_2_4 = "2_4"
    case age_5_7 = "5_7"
    case age_8_10 = "8_10"
    case age_11_plus = "11_plus"
    
    var displayName: String {
        switch self {
        case .age_2_4: return "2-4 years"
        case .age_5_7: return "5-7 years"
        case .age_8_10: return "8-10 years"
        case .age_11_plus: return "11+ years"
        }
    }
}

// MARK: - ReadingMode

enum ReadingMode: String, Codable, CaseIterable, Hashable {
    case visual
    case light_text = "light_text"
    case full_text = "full_text"
    
    var displayName: String {
        switch self {
        case .visual: return "Visual Only"
        case .light_text: return "Icons + Labels"
        case .full_text: return "Full Text"
        }
    }
}

// MARK: - CompletionRule

enum CompletionRule: String, Codable, CaseIterable, Hashable {
    case all_steps_required = "all_steps_required"
    
    var displayName: String {
        switch self {
        case .all_steps_required: return "All Steps Required"
        }
    }
}

// MARK: - EventType

enum EventType: String, Codable, CaseIterable, Hashable {
    case complete
    case undo
}

