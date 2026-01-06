//
//  QRCodeScanner.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import AVFoundation
import UIKit

/// Delegate protocol for QR code scan results
protocol QRCodeScannerDelegate: AnyObject {
    func qrCodeScanner(_ scanner: QRCodeScanner, didScan code: String)
    func qrCodeScanner(_ scanner: QRCodeScanner, didFailWith error: QRCodeScannerError)
}

/// Errors that can occur during QR code scanning
enum QRCodeScannerError: Error {
    case cameraAccessDenied
    case cameraUnavailable
    case scanningFailed
    
    var localizedDescription: String {
        switch self {
        case .cameraAccessDenied:
            return "Camera access is required to scan QR codes. Please enable it in Settings."
        case .cameraUnavailable:
            return "Camera is not available on this device."
        case .scanningFailed:
            return "Failed to scan QR code. Please try again."
        }
    }
}

/// QR code scanner using AVFoundation
final class QRCodeScanner: NSObject {
    weak var delegate: QRCodeScannerDelegate?
    
    private var captureSession: AVCaptureSession?
    private var videoPreviewLayer: AVCaptureVideoPreviewLayer?
    
    /// Start scanning for QR codes
    /// - Parameter view: The view to display the camera preview
    func startScanning(in view: UIView) {
        // Check camera authorization
        let authStatus = AVCaptureDevice.authorizationStatus(for: .video)
        
        switch authStatus {
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                if granted {
                    DispatchQueue.main.async {
                        self?.setupCaptureSession(in: view)
                    }
                } else {
                    self?.delegate?.qrCodeScanner(self!, didFailWith: .cameraAccessDenied)
                }
            }
        case .restricted, .denied:
            delegate?.qrCodeScanner(self, didFailWith: .cameraAccessDenied)
        case .authorized:
            setupCaptureSession(in: view)
        @unknown default:
            delegate?.qrCodeScanner(self, didFailWith: .cameraAccessDenied)
        }
    }
    
    /// Stop scanning
    func stopScanning() {
        captureSession?.stopRunning()
        videoPreviewLayer?.removeFromSuperlayer()
        captureSession = nil
        videoPreviewLayer = nil
    }
    
    private func setupCaptureSession(in view: UIView) {
        guard let device = AVCaptureDevice.default(for: .video) else {
            delegate?.qrCodeScanner(self, didFailWith: .cameraUnavailable)
            return
        }
        
        do {
            let input = try AVCaptureDeviceInput(device: device)
            let captureSession = AVCaptureSession()
            
            if captureSession.canAddInput(input) {
                captureSession.addInput(input)
            } else {
                delegate?.qrCodeScanner(self, didFailWith: .scanningFailed)
                return
            }
            
            let metadataOutput = AVCaptureMetadataOutput()
            
            if captureSession.canAddOutput(metadataOutput) {
                captureSession.addOutput(metadataOutput)
                metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                metadataOutput.metadataObjectTypes = [.qr]
            } else {
                delegate?.qrCodeScanner(self, didFailWith: .scanningFailed)
                return
            }
            
            // Setup preview layer
            let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
            previewLayer.frame = view.layer.bounds
            previewLayer.videoGravity = .resizeAspectFill
            view.layer.addSublayer(previewLayer)
            
            self.captureSession = captureSession
            self.videoPreviewLayer = previewLayer
            
            // Start running on background thread
            DispatchQueue.global(qos: .userInitiated).async {
                captureSession.startRunning()
            }
        } catch {
            delegate?.qrCodeScanner(self, didFailWith: .scanningFailed)
        }
    }
}

// MARK: - AVCaptureMetadataOutputObjectsDelegate

extension QRCodeScanner: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        // Stop scanning after first result
        stopScanning()
        
        guard let metadataObject = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              metadataObject.type == .qr,
              let stringValue = metadataObject.stringValue else {
            delegate?.qrCodeScanner(self, didFailWith: .scanningFailed)
            return
        }
        
        delegate?.qrCodeScanner(self, didScan: stringValue)
    }
}

