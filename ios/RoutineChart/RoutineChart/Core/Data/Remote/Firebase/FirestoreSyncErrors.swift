//
//  FirestoreSyncErrors.swift
//  RoutineChart
//
//  Shared errors for Firestore sync operations
//

import Foundation

enum SyncError: LocalizedError {
    case invalidData
    case networkError
    
    var errorDescription: String? {
        switch self {
        case .invalidData:
            return "Invalid data format from Firestore"
        case .networkError:
            return "Network error during sync"
        }
    }
}

