//
//  AuthRepository.swift
//  RoutineChart
//
//  Created for Phase 2.1: Firebase Auth
//

import Foundation
import Combine

/// Protocol for authentication operations
protocol AuthRepository {
    /// Current authenticated user (nil if not authenticated)
    var currentUser: AuthUser? { get }
    
    /// Publisher that emits when auth state changes
    var authStatePublisher: AnyPublisher<AuthUser?, Never> { get }
    
    /// Sign in with email and password (parent flow)
    func signInWithEmail(email: String, password: String) async throws -> AuthUser
    
    /// Sign up with email and password (parent flow)
    func signUpWithEmail(email: String, password: String) async throws -> AuthUser
    
    /// Sign in anonymously (child flow)
    func signInAnonymously() async throws -> AuthUser
    
    /// Link anonymous account to email (upgrade child to parent)
    func linkAnonymousToEmail(email: String, password: String) async throws -> AuthUser
    
    /// Sign out
    func signOut() throws
    
    /// Send password reset email
    func sendPasswordReset(email: String) async throws
}

