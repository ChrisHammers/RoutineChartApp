package com.HammersTech.RoutineChart.core.utils

/**
 * Utility for generating and validating human-readable invite codes
 * Format: XXX-YYYY (3 letters + 4 numbers, separated by dash)
 * Example: "ABC-1234"
 * Phase 2.2: QR Family Joining (Shareable Codes)
 */
object InviteCodeGenerator {
    private val LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ" // Exclude I, O to avoid confusion with 1, 0
    private val NUMBERS = "23456789" // Exclude 0, 1 to avoid confusion with O, I
    
    /**
     * Generate a short, human-readable invite code
     * @return 8-character invite code (including dash), e.g., "ABC-1234"
     */
    fun generateInviteCode(): String {
        val code = StringBuilder()
        
        // Generate 3 random letters
        repeat(3) {
            code.append(LETTERS.random())
        }
        
        code.append("-")
        
        // Generate 4 random numbers
        repeat(4) {
            code.append(NUMBERS.random())
        }
        
        return code.toString()
    }
    
    /**
     * Validate and normalize invite code format
     * @param code The code to validate (can include spaces, lowercase, etc.)
     * @return Normalized code in XXX-YYYY format, or null if invalid
     */
    fun normalizeInviteCode(code: String): String? {
        // Remove spaces and convert to uppercase
        val cleaned = code.replace(" ", "").uppercase()
        
        // Check if it matches XXX-YYYY or XXXYYY format
        val withDash = if (cleaned.contains("-")) {
            cleaned
        } else {
            insertDash(cleaned)
        }
        
        // Validate format
        if (withDash.length != 8) return null
        
        val parts = withDash.split("-")
        if (parts.size != 2 || parts[0].length != 3 || parts[1].length != 4) {
            return null
        }
        
        // Validate first part is letters
        val letterPart = parts[0]
        if (!letterPart.all { it.isLetter() }) return null
        
        // Validate second part is numbers
        val numberPart = parts[1]
        if (!numberPart.all { it.isDigit() }) return null
        
        return withDash
    }
    
    private fun insertDash(code: String): String {
        if (code.length != 7) return code
        return "${code.substring(0, 3)}-${code.substring(3)}"
    }
}

