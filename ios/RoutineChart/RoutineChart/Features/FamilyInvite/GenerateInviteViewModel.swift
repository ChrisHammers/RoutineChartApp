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
    private let userRepository: UserRepository
    private let authRepository: AuthRepository
    private var timer: Timer?
    private var cancellables = Set<AnyCancellable>()
    private var inviteListener: FirestoreInviteListener?
    
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
    
    deinit {
        timer?.invalidate()
        // Note: inviteListener will clean itself up via its own deinit
        // We can't call stopRealTimeListener() here because deinit is not @MainActor isolated
    }
    
    func loadActiveInvite() async {
        isLoading = true
        loadingMessage = "Loading..."
        errorMessage = nil
        
        do {
            // Get the current user's familyId (source of truth)
            guard let authUser = authRepository.currentUser else {
                isLoading = false
                return
            }
            
            guard let user = try await userRepository.get(id: authUser.id) else {
                AppLogger.ui.error("No user record found for authenticated user: \(authUser.id)")
                isLoading = false
                return
            }
            
            let familyId = user.familyId
            AppLogger.ui.info("Loading invites for user's family: \(familyId)")
            
            // Sync invites from Firestore first (to get invites created on other devices)
            if let compositeRepo = inviteRepository as? CompositeFamilyInviteRepository {
                do {
                    try await compositeRepo.syncFromFirestore(familyId: familyId)
                    AppLogger.ui.info("Synced invites from Firestore for family: \(familyId)")
                } catch {
                    // Log but don't fail - we can still use local data
                    AppLogger.ui.error("Failed to sync invites from Firestore: \(error.localizedDescription)")
                }
            }
            
            // Get all active invites for this family (now includes synced invites)
            let activeInvites = try await inviteRepository.getActiveInvites(familyId: familyId)
            
            // Find the first valid (not expired, not max uses) invite
            if let validInvite = activeInvites.first(where: { $0.isValid }) {
                self.invite = validInvite
                self.qrCodeImage = QRCodeGenerator.generate(for: validInvite)
                startTimer()
                startRealTimeListener(for: validInvite)
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
            // Get the current user's familyId (source of truth)
            guard let authUser = authRepository.currentUser else {
                errorMessage = "Please sign in to generate an invite"
                isLoading = false
                return
            }
            
            guard let user = try await userRepository.get(id: authUser.id) else {
                errorMessage = "No user record found. Please sign in again."
                isLoading = false
                return
            }
            
            let familyId = user.familyId
            AppLogger.ui.info("Generating invite for user's family: \(familyId)")
            
            let createdBy = authUser.id
            
            // Create invite
            let token = TokenGenerator.generateSecureToken()
            let inviteCode = InviteCodeGenerator.generateInviteCode()
            let expiresAt = Calendar.current.date(byAdding: .hour, value: 24, to: Date()) ?? Date().addingTimeInterval(86400)
            
            let newInvite = FamilyInvite(
                familyId: familyId,
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
            
            // Start real-time listener for updates
            startRealTimeListener(for: newInvite)
            
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
            stopRealTimeListener()
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
            // Auto-dismiss expired invites
            stopRealTimeListener()
            self.invite = nil
            self.qrCodeImage = nil
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
    
    // MARK: - Real-time Updates
    
    private func startRealTimeListener(for invite: FamilyInvite) {
        // Stop any existing listener
        stopRealTimeListener()
        
        // Create new listener
        let listener = FirestoreInviteListener()
        self.inviteListener = listener
        
        // Subscribe to updates
        listener.invitePublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] updatedInvite in
                guard let self = self else { return }
                
                if let updatedInvite = updatedInvite {
                    // Update the invite with real-time data
                    self.invite = updatedInvite
                    
                    // If invite is no longer valid (expired, deactivated, max uses), clear it
                    if !updatedInvite.isValid {
                        AppLogger.ui.info("Invite \(updatedInvite.id) is no longer valid - clearing")
                        self.stopRealTimeListener()
                        self.invite = nil
                        self.qrCodeImage = nil
                        self.timer?.invalidate()
                    }
                } else {
                    // Invite was deleted or doesn't exist
                    AppLogger.ui.info("Invite was deleted or doesn't exist - clearing")
                    self.stopRealTimeListener()
                    self.invite = nil
                    self.qrCodeImage = nil
                    self.timer?.invalidate()
                }
            }
            .store(in: &cancellables)
        
        // Start listening
        listener.startListening(inviteId: invite.id, familyId: invite.familyId)
        AppLogger.ui.info("Started real-time listener for invite: \(invite.id)")
    }
    
    private func stopRealTimeListener() {
        inviteListener?.stopListening()
        inviteListener = nil
    }
}

