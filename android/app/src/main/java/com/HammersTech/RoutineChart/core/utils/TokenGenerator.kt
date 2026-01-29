package com.HammersTech.RoutineChart.core.utils

import android.util.Base64
import java.security.SecureRandom

/**
 * Token generator utility
 * Phase 2.2: QR Family Joining
 */
object TokenGenerator {
    /**
     * Generate a cryptographically secure random token
     * @param byteCount Number of random bytes (default: 32)
     * @return Base64-encoded secure token string
     */
    fun generateSecureToken(byteCount: Int = 32): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Generate a URL-safe token (alphanumeric only)
     * @param length Desired token length (default: 32)
     * @return URL-safe token string
     */
    fun generateURLSafeToken(length: Int = 32): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}
