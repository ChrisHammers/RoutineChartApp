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
    @Published var loadingMessage = "Loading..."
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
    
    func loadActiveInvite() async {
        isLoading = true
        loadingMessage = "Loading..."
        errorMessage = nil
        
        do {
            // Get the current family
            guard let family = try await familyRepository.getAll().first else {
                isLoading = false
                return
            }
            
            // Get all active invites for this family
            let activeInvites = try await inviteRepository.getActiveInvites(familyId: family.id)
            
            // Find the first valid (not expired, not max uses) invite
            if let validInvite = activeInvites.first(where: { $0.isValid }) {
                self.invite = validInvite
                self.qrCodeImage = QRCodeGenerator.generate(for: validInvite)
                startTimer()
                AppLogger.ui.info("Loaded existing active invite: \(validInvite.id)")
            }
        } catch {
            AppLogger.ui.error("Failed to load active invite: \(error.localizedDescription)")
            // Don't show error to user - just proceed to generate new one if needed
        }
        
        isLoading = false
    }
    
    func generateInvite() async {
        isLoading = true
        loadingMessage = "Generating invite..."
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
    
    func shareableImage(for invite: FamilyInvite, qrImage: UIImage) -> UIImage? {
        // Create a composite image with QR code and invite code text
        let size = CGSize(width: 600, height: 800)
        let renderer = UIGraphicsImageRenderer(size: size)
        
        return renderer.image { context in
            // White background
            UIColor.white.setFill()
            context.fill(CGRect(origin: .zero, size: size))
            
            // Title text
            let titleText = "Join my family on Routine Chart!"
            let titleAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 28, weight: .bold),
                .foregroundColor: UIColor.label
            ]
            let titleSize = titleText.size(withAttributes: titleAttributes)
            let titleRect = CGRect(
                x: (size.width - titleSize.width) / 2,
                y: 40,
                width: titleSize.width,
                height: titleSize.height
            )
            titleText.draw(in: titleRect, withAttributes: titleAttributes)
            
            // Invite code text
            let codeText = "Invite Code: \(invite.inviteCode)"
            let codeAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.monospacedSystemFont(ofSize: 32, weight: .bold),
                .foregroundColor: UIColor.systemBlue
            ]
            let codeSize = codeText.size(withAttributes: codeAttributes)
            let codeRect = CGRect(
                x: (size.width - codeSize.width) / 2,
                y: titleRect.maxY + 30,
                width: codeSize.width,
                height: codeSize.height
            )
            codeText.draw(in: codeRect, withAttributes: codeAttributes)
            
            // QR Code (centered, larger)
            let qrSize: CGFloat = 400
            let qrRect = CGRect(
                x: (size.width - qrSize) / 2,
                y: codeRect.maxY + 40,
                width: qrSize,
                height: qrSize
            )
            qrImage.draw(in: qrRect)
            
            // Instructions text
            let instructionText = "Scan the QR code or use the invite code above"
            let instructionAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 18, weight: .regular),
                .foregroundColor: UIColor.secondaryLabel
            ]
            let instructionSize = instructionText.size(withAttributes: instructionAttributes)
            let instructionRect = CGRect(
                x: (size.width - instructionSize.width) / 2,
                y: qrRect.maxY + 30,
                width: instructionSize.width,
                height: instructionSize.height
            )
            instructionText.draw(in: instructionRect, withAttributes: instructionAttributes)
        }
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

