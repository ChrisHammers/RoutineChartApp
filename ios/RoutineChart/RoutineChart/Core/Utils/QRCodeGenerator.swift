//
//  QRCodeGenerator.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import UIKit
import CoreImage.CIFilterBuiltins

struct QRCodeGenerator {
    /// Generate a QR code image from a string
    /// - Parameters:
    ///   - string: The string to encode
    ///   - size: The desired size of the QR code (default: 300x300)
    /// - Returns: UIImage containing the QR code, or nil if generation fails
    static func generate(from string: String, size: CGSize = CGSize(width: 300, height: 300)) -> UIImage? {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        
        guard let data = string.data(using: .utf8) else {
            return nil
        }
        
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel") // Medium error correction
        
        guard let outputImage = filter.outputImage else {
            return nil
        }
        
        // Scale the QR code to the desired size
        let scaleX = size.width / outputImage.extent.width
        let scaleY = size.height / outputImage.extent.height
        let scaledImage = outputImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY))
        
        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else {
            return nil
        }
        
        return UIImage(cgImage: cgImage)
    }
    
    /// Generate a QR code image from a URL
    /// - Parameters:
    ///   - url: The URL to encode
    ///   - size: The desired size of the QR code (default: 300x300)
    /// - Returns: UIImage containing the QR code, or nil if generation fails
    static func generate(from url: URL, size: CGSize = CGSize(width: 300, height: 300)) -> UIImage? {
        return generate(from: url.absoluteString, size: size)
    }
    
    /// Generate a QR code for a family invite
    /// - Parameters:
    ///   - invite: The FamilyInvite to generate QR code for
    ///   - size: The desired size of the QR code (default: 300x300)
    /// - Returns: UIImage containing the QR code, or nil if generation fails
    static func generate(for invite: FamilyInvite, size: CGSize = CGSize(width: 300, height: 300)) -> UIImage? {
        guard let url = invite.qrCodeURL() else {
            return nil
        }
        return generate(from: url, size: size)
    }
}

