//
//  JoinWithCodeViewModel.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining (Manual Code Entry)
//

import Foundation
import Combine
import OSLog

@MainActor
final class JoinWithCodeViewModel: ObservableObject {
    @Published var inviteCode: String = ""
    @Published var errorMessage: String?
    @Published var isJoining = false
    @Published var joinSuccess = false
    
    private let inviteRepository: FamilyInviteRepository
    private let familyRepository: FamilyRepository
    private let userRepository: UserRepository
    private let authRepository: AuthRepository
    
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
    
    var isCodeValid: Bool {
        InviteCodeGenerator.normalizeInviteCode(inviteCode) != nil
    }
    
    func joinWithCode() async {
        errorMessage = nil
        
        guard let normalizedCode = InviteCodeGenerator.normalizeInviteCode(inviteCode) else {
            errorMessage = "Invalid code format. Use XXX-YYYY (e.g., ABC-1234)"
            return
        }
        
        isJoining = true
        
        do {
            // Find invite by code
            guard let invite = try await inviteRepository.getByInviteCode(normalizedCode) else {
                errorMessage = "Invite code not found. Please check the code and try again."
                isJoining = false
                return
            }
            
            // Validate invite
            guard invite.isValid else {
                if invite.isExpired {
                    errorMessage = "This invite has expired"
                } else if invite.isMaxUsesReached {
                    errorMessage = "This invite has reached its maximum uses"
                } else {
                    errorMessage = "This invite is no longer active"
                }
                isJoining = false
                return
            }
            
            // Get the family
            guard let family = try await familyRepository.get(id: invite.familyId) else {
                errorMessage = "Family not found"
                isJoining = false
                return
            }
            
            // Check if user is authenticated
            guard let authUser = authRepository.currentUser else {
                errorMessage = "Please sign in to join a family"
                isJoining = false
                return
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
            
            joinSuccess = true
            isJoining = false
        } catch {
            errorMessage = "Failed to join family: \(error.localizedDescription)"
            AppLogger.ui.error("Failed to join family with code: \(error)")
            isJoining = false
        }
    }
}

