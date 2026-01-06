//
//  InviteCodeGenerator.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining (Shareable Codes)
//

import Foundation

struct InviteCodeGenerator {
    /// Generate a short, human-readable invite code
    /// Format: XXX-YYYY (3 letters + 4 numbers, separated by dash)
    /// Example: "ABC-1234"
    /// - Returns: 8-character invite code (including dash)
    static func generateInviteCode() -> String {
        let letters = "ABCDEFGHJKLMNPQRSTUVWXYZ" // Exclude I, O to avoid confusion with 1, 0
        let numbers = "23456789" // Exclude 0, 1 to avoid confusion with O, I
        
        var code = ""
        
        // Generate 3 random letters
        for _ in 0..<3 {
            let randomIndex = Int.random(in: 0..<letters.count)
            let randomLetter = letters[letters.index(letters.startIndex, offsetBy: randomIndex)]
            code.append(randomLetter)
        }
        
        code.append("-")
        
        // Generate 4 random numbers
        for _ in 0..<4 {
            let randomIndex = Int.random(in: 0..<numbers.count)
            let randomNumber = numbers[numbers.index(numbers.startIndex, offsetBy: randomIndex)]
            code.append(randomNumber)
        }
        
        return code
    }
    
    /// Validate and normalize invite code format
    /// - Parameter code: The code to validate (can include spaces, lowercase, etc.)
    /// - Returns: Normalized code in XXX-YYYY format, or nil if invalid
    static func normalizeInviteCode(_ code: String) -> String? {
        // Remove spaces and convert to uppercase
        let cleaned = code.replacingOccurrences(of: " ", with: "").uppercased()
        
        // Check if it matches XXX-YYYY or XXXYYY format
        let withDash = cleaned.contains("-") ? cleaned : insertDash(in: cleaned)
        
        // Validate format
        guard withDash.count == 8 else { return nil }
        
        let parts = withDash.split(separator: "-")
        guard parts.count == 2,
              parts[0].count == 3,
              parts[1].count == 4 else {
            return nil
        }
        
        // Validate first part is letters
        let letterPart = String(parts[0])
        guard letterPart.allSatisfy({ $0.isLetter }) else { return nil }
        
        // Validate second part is numbers
        let numberPart = String(parts[1])
        guard numberPart.allSatisfy({ $0.isNumber }) else { return nil }
        
        return withDash
    }
    
    private static func insertDash(in code: String) -> String {
        guard code.count == 7 else { return code }
        let index = code.index(code.startIndex, offsetBy: 3)
        return String(code[..<index]) + "-" + String(code[index...])
    }
}

