//
//  AuthUser.swift
//  RoutineChart
//
//  Created for Phase 2.1: Firebase Auth
//

import Foundation

/// Represents an authenticated user (separate from domain User)
struct AuthUser: Identifiable {
    let id: String  // Firebase UID
    let email: String?
    let isAnonymous: Bool
    
    init(id: String, email: String?, isAnonymous: Bool = false) {
        self.id = id
        self.email = email
        self.isAnonymous = isAnonymous
    }
}

