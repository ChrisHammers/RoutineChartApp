//
//  GenerateInviteViewModel.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import Foundation
import UIKit
import Combine
import OSLog

@MainActor
final class GenerateInviteViewModel: ObservableObject {
    @Published var qrCodeImage: UIImage?
    @Published var invite: FamilyInvite?
    @Published var errorMessage: String?
    @Published var isLoading = false
    @Published var timeRemaining: String = ""
    
    private let inviteRepository: FamilyInviteRepository
    private let familyRepository: FamilyRepository
    private var timer: Timer?
    private var cancellables = Set<AnyCancellable>()
    
    init(
        inviteRepository: FamilyInviteRepository,
        familyRepository: FamilyRepository
    ) {
        self.inviteRepository = inviteRepository
        self.familyRepository = familyRepository
    }
    
    deinit {
        timer?.invalidate()
    }
    
    func generateInvite() async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Get the current family
            guard let family = try await familyRepository.getAll().first else {
                errorMessage = "No family found. Please create a family first."
                isLoading = false
                return
            }
            
            // For now, use a placeholder user ID (in Phase 2.3, use actual auth user)
            let createdBy = "currentUserId" // TODO: Replace with actual auth user ID
            
            // Create invite
            let token = TokenGenerator.generateSecureToken()
            let inviteCode = InviteCodeGenerator.generateInviteCode()
            let expiresAt = Calendar.current.date(byAdding: .hour, value: 24, to: Date()) ?? Date().addingTimeInterval(86400)
            
            let newInvite = FamilyInvite(
                familyId: family.id,
                token: token,
                inviteCode: inviteCode,
                createdBy: createdBy,
                expiresAt: expiresAt
            )
            
            try await inviteRepository.create(newInvite)
            self.invite = newInvite
            
            // Generate QR code
            self.qrCodeImage = QRCodeGenerator.generate(for: newInvite)
            
            // Start timer to update time remaining
            startTimer()
            
            AppLogger.ui.info("Generated family invite: \(newInvite.id)")
        } catch {
            errorMessage = "Failed to generate invite: \(error.localizedDescription)"
            AppLogger.ui.error("Failed to generate invite: \(error)")
        }
        
        isLoading = false
    }
    
    func deactivateInvite() async {
        guard let invite = invite else { return }
        
        do {
            try await inviteRepository.deactivate(id: invite.id)
            self.invite = nil
            self.qrCodeImage = nil
            timer?.invalidate()
            AppLogger.ui.info("Deactivated invite: \(invite.id)")
        } catch {
            errorMessage = "Failed to deactivate invite: \(error.localizedDescription)"
            AppLogger.ui.error("Failed to deactivate invite: \(error)")
        }
    }
    
    func shareInvite() {
        guard let url = invite?.qrCodeURL() else { return }
        
        // TODO: Implement sharing functionality
        AppLogger.ui.info("Share invite URL: \(url)")
    }
    
    private func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.updateTimeRemaining()
            }
        }
        updateTimeRemaining()
    }
    
    private func updateTimeRemaining() {
        guard let invite = invite else {
            timeRemaining = ""
            return
        }
        
        let remaining = invite.timeRemaining
        if remaining <= 0 {
            timeRemaining = "Expired"
            timer?.invalidate()
            return
        }
        
        let hours = Int(remaining) / 3600
        let minutes = (Int(remaining) % 3600) / 60
        let seconds = Int(remaining) % 60
        
        if hours > 0 {
            timeRemaining = String(format: "Expires in %dh %dm", hours, minutes)
        } else if minutes > 0 {
            timeRemaining = String(format: "Expires in %dm %ds", minutes, seconds)
        } else {
            timeRemaining = String(format: "Expires in %ds", seconds)
        }
    }
}

