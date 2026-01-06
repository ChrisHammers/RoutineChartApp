//
//  FamilyInvite.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import Foundation
import ULID

struct FamilyInvite: Identifiable, Codable {
    let id: String              // ULID
    let familyId: String
    let token: String           // Secure random token
    let inviteCode: String      // Human-readable code (e.g., "ABC-1234")
    let createdBy: String       // userId who created invite
    let createdAt: Date
    let expiresAt: Date
    let maxUses: Int?           // Optional limit on uses
    let usedCount: Int          // How many times used
    let isActive: Bool          // Can be deactivated
    
    init(
        id: String = ULID().ulidString,
        familyId: String,
        token: String,
        inviteCode: String,
        createdBy: String,
        createdAt: Date = Date(),
        expiresAt: Date,
        maxUses: Int? = nil,
        usedCount: Int = 0,
        isActive: Bool = true
    ) {
        self.id = id
        self.familyId = familyId
        self.token = token
        self.inviteCode = inviteCode
        self.createdBy = createdBy
        self.createdAt = createdAt
        self.expiresAt = expiresAt
        self.maxUses = maxUses
        self.usedCount = usedCount
        self.isActive = isActive
    }
    
    /// Check if invite is valid for use
    var isValid: Bool {
        isActive && !isExpired && !isMaxUsesReached
    }
    
    /// Check if invite has expired
    var isExpired: Bool {
        Date() > expiresAt
    }
    
    /// Check if max uses reached
    var isMaxUsesReached: Bool {
        guard let maxUses = maxUses else { return false }
        return usedCount >= maxUses
    }
    
    /// Time remaining until expiration
    var timeRemaining: TimeInterval {
        expiresAt.timeIntervalSince(Date())
    }
    
    /// Generate QR code URL
    func qrCodeURL() -> URL? {
        var components = URLComponents()
        components.scheme = "routinechart"
        components.host = "join"
        components.queryItems = [
            URLQueryItem(name: "familyId", value: familyId),
            URLQueryItem(name: "token", value: token),
            URLQueryItem(name: "expires", value: String(Int(expiresAt.timeIntervalSince1970)))
        ]
        return components.url
    }
    
    /// Parse invite from QR code URL
    static func from(url: URL) -> (familyId: String, token: String, expires: Date)? {
        guard url.scheme == "routinechart",
              url.host == "join",
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let queryItems = components.queryItems else {
            return nil
        }
        
        var familyId: String?
        var token: String?
        var expires: Date?
        
        for item in queryItems {
            switch item.name {
            case "familyId":
                familyId = item.value
            case "token":
                token = item.value
            case "expires":
                if let timestamp = item.value, let timestampInt = Int(timestamp) {
                    expires = Date(timeIntervalSince1970: TimeInterval(timestampInt))
                }
            default:
                break
            }
        }
        
        guard let familyId = familyId,
              let token = token,
              let expires = expires else {
            return nil
        }
        
        return (familyId, token, expires)
    }
}

