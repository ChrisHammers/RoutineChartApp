//
//  FirestoreSyncErrors.swift
//  RoutineChart
//
//  Shared errors for Firestore sync operations
//

import Foundation

enum SyncError: LocalizedError {
    case invalidData(String)
    case networkError
    
    var errorDescription: String? {
        switch self {
        case .invalidData(let error):
            return "Invalid data format from Firestore: /(error)"
        case .networkError:
            return "Network error during sync"
        }
    }
}

