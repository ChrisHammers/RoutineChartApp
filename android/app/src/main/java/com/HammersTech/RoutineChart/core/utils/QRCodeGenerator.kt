package com.HammersTech.RoutineChart.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite

/**
 * QR Code generator using ZXing
 * Phase 2.2: QR Family Joining
 */
object QRCodeGenerator {
    /**
     * Generate a QR code bitmap from a string
     * @param string The string to encode
     * @param width The desired width in pixels (default: 512)
     * @param height The desired height in pixels (default: 512)
     * @return Bitmap containing the QR code, or null if generation fails
     */
    fun generate(
        string: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1 // Minimal margin
            )
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(string, BarcodeFormat.QR_CODE, width, height, hints)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            AppLogger.UI.error("Failed to generate QR code", e)
            null
        }
    }
    
    /**
     * Generate a QR code bitmap for a family invite
     * @param invite The FamilyInvite to generate QR code for
     * @param width The desired width in pixels (default: 512)
     * @param height The desired height in pixels (default: 512)
     * @return Bitmap containing the QR code, or null if generation fails
     */
    fun generate(
        invite: FamilyInvite,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        return generate(invite.qrCodeURL(), width, height)
    }
}

