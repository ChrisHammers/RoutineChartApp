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
    
    init(
        inviteRepository: FamilyInviteRepository,
        familyRepository: FamilyRepository,
        userRepository: UserRepository
    ) {
        self.inviteRepository = inviteRepository
        self.familyRepository = familyRepository
        self.userRepository = userRepository
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
            
            // TODO Phase 2.3: Link current auth user to this family
            AppLogger.ui.info("Successfully validated invite for family: \(family.name ?? family.id)")
            
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

