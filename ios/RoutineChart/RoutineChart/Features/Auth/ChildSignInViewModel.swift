//
//  ChildSignInViewModel.swift
//  RoutineChart
//
//  Created for Phase 2.1: Firebase Auth
//

import Foundation
import Combine

@MainActor
final class ChildSignInViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let authRepository: AuthRepository
    
    init(authRepository: AuthRepository) {
        self.authRepository = authRepository
    }
    
    func signInAsChild() async {
        isLoading = true
        errorMessage = nil
        
        do {
            _ = try await authRepository.signInAnonymously()
            AppLogger.log("Child signed in anonymously")
        } catch {
            AppLogger.error("Child sign in failed", error: error)
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
}

