//
//  TokenGenerator.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import Foundation
import CryptoKit

struct TokenGenerator {
    /// Generate a cryptographically secure random token
    /// - Parameter byteCount: Number of random bytes (default: 32)
    /// - Returns: Base64-encoded secure token string
    static func generateSecureToken(byteCount: Int = 32) -> String {
        var bytes = [UInt8](repeating: 0, count: byteCount)
        let result = SecRandomCopyBytes(kSecRandomDefault, byteCount, &bytes)
        
        guard result == errSecSuccess else {
            // Fallback to SymmetricKey if SecRandomCopyBytes fails
            let key = SymmetricKey(size: .bits256)
            return key.withUnsafeBytes { Data($0).base64EncodedString() }
        }
        
        return Data(bytes).base64EncodedString()
    }
    
    /// Generate a URL-safe token (alphanumeric only)
    /// - Parameter length: Desired token length (default: 32)
    /// - Returns: URL-safe token string
    static func generateURLSafeToken(length: Int = 32) -> String {
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var token = ""
        
        for _ in 0..<length {
            let randomIndex = Int.random(in: 0..<letters.count)
            let randomCharacter = letters[letters.index(letters.startIndex, offsetBy: randomIndex)]
            token.append(randomCharacter)
        }
        
        return token
    }
}

