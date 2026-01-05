//
//  FirebaseAuthService.swift
//  RoutineChart
//
//  Created for Phase 2.1: Firebase Auth
//

import Foundation
import FirebaseAuth
import Combine

/// Firebase implementation of AuthRepository
final class FirebaseAuthService: AuthRepository {
    private let auth = Auth.auth()
    private let authStateSubject = CurrentValueSubject<AuthUser?, Never>(nil)
    
    var currentUser: AuthUser? {
        guard let firebaseUser = auth.currentUser else { return nil }
        return AuthUser(
            id: firebaseUser.uid,
            email: firebaseUser.email,
            isAnonymous: firebaseUser.isAnonymous
        )
    }
    
    var authStatePublisher: AnyPublisher<AuthUser?, Never> {
        authStateSubject.eraseToAnyPublisher()
    }
    
    init() {
        // Initialize with current auth state
        authStateSubject.send(currentUser)
        
        // Listen for auth state changes
        auth.addStateDidChangeListener { [weak self] _, user in
            guard let self = self else { return }
            if let user = user {
                self.authStateSubject.send(AuthUser(
                    id: user.uid,
                    email: user.email,
                    isAnonymous: user.isAnonymous
                ))
            } else {
                self.authStateSubject.send(nil)
            }
        }
    }
    
    func signInWithEmail(email: String, password: String) async throws -> AuthUser {
        let result = try await auth.signIn(withEmail: email, password: password)
        return AuthUser(
            id: result.user.uid,
            email: result.user.email,
            isAnonymous: result.user.isAnonymous
        )
    }
    
    func signUpWithEmail(email: String, password: String) async throws -> AuthUser {
        let result = try await auth.createUser(withEmail: email, password: password)
        return AuthUser(
            id: result.user.uid,
            email: result.user.email,
            isAnonymous: result.user.isAnonymous
        )
    }
    
    func signInAnonymously() async throws -> AuthUser {
        let result = try await auth.signInAnonymously()
        return AuthUser(
            id: result.user.uid,
            email: result.user.email,
            isAnonymous: result.user.isAnonymous
        )
    }
    
    func linkAnonymousToEmail(email: String, password: String) async throws -> AuthUser {
        guard let currentUser = auth.currentUser, currentUser.isAnonymous else {
            throw AuthError.notAnonymous
        }
        
        let credential = EmailAuthProvider.credential(withEmail: email, password: password)
        let result = try await currentUser.link(with: credential)
        
        return AuthUser(
            id: result.user.uid,
            email: result.user.email,
            isAnonymous: result.user.isAnonymous
        )
    }
    
    func signOut() throws {
        try auth.signOut()
    }
    
    func sendPasswordReset(email: String) async throws {
        try await auth.sendPasswordReset(withEmail: email)
    }
}

// MARK: - Errors
enum AuthError: LocalizedError {
    case notAnonymous
    case invalidCredentials
    case userNotFound
    
    var errorDescription: String? {
        switch self {
        case .notAnonymous:
            return "Current user is not anonymous"
        case .invalidCredentials:
            return "Invalid email or password"
        case .userNotFound:
            return "User not found"
        }
    }
}

