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
    private var cancellables = Set<AnyCancellable>()
    
    init(
        inviteRepository: FamilyInviteRepository,
        familyRepository: FamilyRepository,
        userRepository: UserRepository
    ) {
        self.inviteRepository = inviteRepository
        self.familyRepository = familyRepository
        self.userRepository = userRepository
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
            
            // TODO Phase 2.3: Link current auth user to this family
            // For now, just log success
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
            
            return true
        } catch {
            errorMessage = "Failed to join family: \(error.localizedDescription)"
            AppLogger.ui.error("Failed to join family: \(error)")
            return false
        }
    }
}

