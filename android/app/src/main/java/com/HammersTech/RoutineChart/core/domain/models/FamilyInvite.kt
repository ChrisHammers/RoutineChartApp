package com.HammersTech.RoutineChart.core.domain.models

import android.net.Uri
import java.time.Instant

/**
 * FamilyInvite domain model
 * Phase 2.2: QR Family Joining
 */
data class FamilyInvite(
    val id: String,                // ULID
    val familyId: String,
    val token: String,             // Secure random token
    val createdBy: String,         // userId who created invite
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant,
    val maxUses: Int? = null,      // Optional limit on uses
    val usedCount: Int = 0,        // How many times used
    val isActive: Boolean = true   // Can be deactivated
) {
    /**
     * Check if invite is valid for use
     */
    val isValid: Boolean
        get() = isActive && !isExpired && !isMaxUsesReached
    
    /**
     * Check if invite has expired
     */
    val isExpired: Boolean
        get() = Instant.now().isAfter(expiresAt)
    
    /**
     * Check if max uses reached
     */
    val isMaxUsesReached: Boolean
        get() = maxUses?.let { usedCount >= it } ?: false
    
    /**
     * Time remaining until expiration (in seconds)
     */
    val timeRemaining: Long
        get() = expiresAt.epochSecond - Instant.now().epochSecond
    
    /**
     * Generate QR code URL
     */
    fun qrCodeURL(): String {
        return Uri.Builder()
            .scheme("routinechart")
            .authority("join")
            .appendQueryParameter("familyId", familyId)
            .appendQueryParameter("token", token)
            .appendQueryParameter("expires", expiresAt.epochSecond.toString())
            .build()
            .toString()
    }
    
    companion object {
        /**
         * Parse invite from QR code URL
         */
        fun fromURL(url: String): Triple<String, String, Instant>? {
            return try {
                val uri = Uri.parse(url)
                if (uri.scheme != "routinechart" || uri.authority != "join") {
                    return null
                }
                
                val familyId = uri.getQueryParameter("familyId") ?: return null
                val token = uri.getQueryParameter("token") ?: return null
                val expiresStr = uri.getQueryParameter("expires") ?: return null
                val expires = Instant.ofEpochSecond(expiresStr.toLong())
                
                Triple(familyId, token, expires)
            } catch (e: Exception) {
                null
            }
        }
    }
}

