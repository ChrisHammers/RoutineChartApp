//
//  ScanInviteViewModel.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import Foundation
import Combine
import OSLog

@MainActor
final class ScanInviteViewModel: ObservableObject {
    @Published var scannedInvite: (familyId: String, token: String, expires: Date)?
    @Published var errorMessage: String?
    @Published var showConfirmation = false
    
    private let inviteRepository: FamilyInviteRepository
    private let familyRepository: FamilyRepository
    private let userRepository: UserRepository
    private let authRepository: AuthRepository
    private var cancellables = Set<AnyCancellable>()
    
    init(
        inviteRepository: FamilyInviteRepository,
        familyRepository: FamilyRepository,
        userRepository: UserRepository,
        authRepository: AuthRepository
    ) {
        self.inviteRepository = inviteRepository
        self.familyRepository = familyRepository
        self.userRepository = userRepository
        self.authRepository = authRepository
    }
    
    func handleScannedCode(_ code: String) {
        guard let url = URL(string: code),
              let parsed = FamilyInvite.from(url: url) else {
            errorMessage = "Invalid QR code. Please scan a valid family invite."
            return
        }
        
        // Check if expired
        if Date() > parsed.expires {
            errorMessage = "This invite has expired. Please ask for a new one."
            return
        }
        
        scannedInvite = parsed
        showConfirmation = true
    }
    
    func joinFamily() async -> Bool {
        guard let scannedInvite = scannedInvite else {
            errorMessage = "No invite scanned"
            return false
        }
        
        do {
            // Validate invite exists and is valid
            guard let invite = try await inviteRepository.getByToken(scannedInvite.token) else {
                errorMessage = "Invalid invite token"
                return false
            }
            
            guard invite.isValid else {
                if invite.isExpired {
                    errorMessage = "This invite has expired"
                } else if invite.isMaxUsesReached {
                    errorMessage = "This invite has reached its maximum uses"
                } else {
                    errorMessage = "This invite is no longer active"
                }
                return false
            }
            
            // Get the family
            guard let family = try await familyRepository.get(id: invite.familyId) else {
                errorMessage = "Family not found"
                return false
            }
            
            // Check if user is authenticated, if not, sign in anonymously
            var authUser = authRepository.currentUser
            if authUser == nil {
                // User is not authenticated - sign in anonymously first
                do {
                    authUser = try await authRepository.signInAnonymously()
                    AppLogger.ui.info("Signed in anonymously for join family flow (QR)")
                } catch {
                    errorMessage = "Failed to sign in: \(error.localizedDescription)"
                    return false
                }
            }
            
            guard let authUser = authUser else {
                errorMessage = "Please sign in to join a family"
                return false
            }
            
            // Link user to family
            // Check if user exists in database
            if let existingUser = try await userRepository.get(id: authUser.id) {
                // User exists - update familyId
                if existingUser.familyId != invite.familyId {
                    // User is switching families
                    try await userRepository.updateFamilyId(userId: authUser.id, familyId: invite.familyId)
                    AppLogger.ui.info("User \(authUser.id) switched to family \(invite.familyId)")
                } else {
                    // User already in this family
                    AppLogger.ui.info("User \(authUser.id) already in family \(invite.familyId)")
                }
            } else {
                // User doesn't exist - create new User record
                // Default to child role for new users joining via invite
                let newUser = User(
                    id: authUser.id,
                    familyId: invite.familyId,
                    role: .child,
                    displayName: authUser.email?.components(separatedBy: "@").first ?? "User",
                    email: authUser.email,
                    createdAt: Date()
                )
                try await userRepository.create(newUser)
                AppLogger.ui.info("Created user \(authUser.id) and linked to family \(invite.familyId)")
            }
            
            // Increment invite used count
            let updatedInvite = FamilyInvite(
                id: invite.id,
                familyId: invite.familyId,
                token: invite.token,
                inviteCode: invite.inviteCode,
                createdBy: invite.createdBy,
                createdAt: invite.createdAt,
                expiresAt: invite.expiresAt,
                maxUses: invite.maxUses,
                usedCount: invite.usedCount + 1,
                isActive: invite.isActive
            )
            try await inviteRepository.update(updatedInvite)
            
            return true
        } catch {
            errorMessage = "Failed to join family: \(error.localizedDescription)"
            AppLogger.ui.error("Failed to join family: \(error)")
            return false
        }
    }
}

