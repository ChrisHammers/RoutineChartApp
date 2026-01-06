//
//  ParentSignInViewModel.swift
//  RoutineChart
//
//  Created for Phase 2.1: Firebase Auth
//

import Foundation
import Combine
import OSLog

@MainActor
final class ParentSignInViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isSignUpMode = false
    
    private let authRepository: AuthRepository
    
    init(authRepository: AuthRepository) {
        self.authRepository = authRepository
    }
    
    var canSubmit: Bool {
        !email.isEmpty && !password.isEmpty && password.count >= 6
    }
    
    func signIn() async {
        guard canSubmit else { return }
        
        isLoading = true
        errorMessage = nil
        
        do {
            if isSignUpMode {
                _ = try await authRepository.signUpWithEmail(email: email, password: password)
                AppLogger.log("Parent signed up successfully")
            } else {
                _ = try await authRepository.signInWithEmail(email: email, password: password)
                AppLogger.log("Parent signed in successfully")
            }
        } catch {
            AppLogger.error("Parent sign in failed: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func toggleMode() {
        isSignUpMode.toggle()
        errorMessage = nil
    }
    
    func sendPasswordReset() async {
        guard !email.isEmpty else {
            errorMessage = "Please enter your email address"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            try await authRepository.sendPasswordReset(email: email)
            errorMessage = "Password reset email sent! Check your inbox."
            AppLogger.log("Password reset email sent to: \(email)")
        } catch {
            AppLogger.error("Password reset failed: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
}

