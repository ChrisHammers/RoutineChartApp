//
//  ScanInviteView.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import SwiftUI
import OSLog

struct ScanInviteView: View {
    @StateObject private var viewModel: ScanInviteViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var scanner: QRCodeScanner?
    @State private var isScanning = false
    @State private var showJoinSuccess = false
    
    init(dependencies: AppDependencies) {
        _viewModel = StateObject(
            wrappedValue: ScanInviteViewModel(
                inviteRepository: dependencies.familyInviteRepository,
                familyRepository: dependencies.familyRepository,
                userRepository: dependencies.userRepository,
                authRepository: dependencies.authRepository
            )
        )
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                if isScanning {
                    CameraPreviewView(scanner: $scanner, onCodeScanned: { code in
                        viewModel.handleScannedCode(code)
                        isScanning = false
                    })
                    .ignoresSafeArea()
                    
                    // Scanner overlay
                    VStack {
                        Spacer()
                        
                        VStack(spacing: 16) {
                            Text("Point camera at QR code")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                                .background(Color.black.opacity(0.7))
                                .cornerRadius(8)
                            
                            Button("Cancel") {
                                isScanning = false
                                scanner?.stopScanning()
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(Color.red.opacity(0.8))
                            .cornerRadius(8)
                        }
                        .padding(.bottom, 50)
                    }
                } else {
                    VStack(spacing: 24) {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 80))
                            .foregroundColor(.accentColor)
                        
                        Text("Scan QR Code")
                            .font(.title2)
                            .fontWeight(.semibold)
                        
                        Text("Scan the QR code shown by your family member to join their family")
                            .font(.body)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button(action: {
                            scanner = QRCodeScanner()
                            isScanning = true
                        }) {
                            Label("Start Scanning", systemImage: "camera")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .padding(.horizontal)
                        
                        if let error = viewModel.errorMessage {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(.red)
                                .padding()
                                .background(Color.red.opacity(0.1))
                                .cornerRadius(8)
                                .padding(.horizontal)
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Join Family")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $viewModel.showConfirmation) {
                JoinConfirmationView(
                    viewModel: viewModel,
                    onDismiss: { success in
                        if success {
                            showJoinSuccess = true
                        }
                        dismiss()
                    }
                )
            }
            .alert("Success!", isPresented: $showJoinSuccess) {
                Button("OK") {
                    dismiss()
                }
            } message: {
                Text("You've successfully joined the family!")
            }
        }
    }
}

// MARK: - Camera Preview View

struct CameraPreviewView: UIViewRepresentable {
    @Binding var scanner: QRCodeScanner?
    let onCodeScanned: (String) -> Void
    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.backgroundColor = .black
        
        let scanner = QRCodeScanner()
        scanner.delegate = context.coordinator
        self.scanner = scanner
        
        DispatchQueue.main.async {
            scanner.startScanning(in: view)
        }
        
        return view
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(onCodeScanned: onCodeScanned)
    }
    
    class Coordinator: NSObject, QRCodeScannerDelegate {
        let onCodeScanned: (String) -> Void
        
        init(onCodeScanned: @escaping (String) -> Void) {
            self.onCodeScanned = onCodeScanned
        }
        
        func qrCodeScanner(_ scanner: QRCodeScanner, didScan code: String) {
            onCodeScanned(code)
        }
        
        func qrCodeScanner(_ scanner: QRCodeScanner, didFailWith error: QRCodeScannerError) {
            AppLogger.ui.error("QR Scanner error: \(error.localizedDescription)")
        }
    }
}

// MARK: - Join Confirmation View

struct JoinConfirmationView: View {
    @ObservedObject var viewModel: ScanInviteViewModel
    let onDismiss: (Bool) -> Void
    @State private var isJoining = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Image(systemName: "person.2.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.accentColor)
                
                Text("Join Family?")
                    .font(.title)
                    .fontWeight(.bold)
                
                VStack(spacing: 8) {
                    Text("You'll be able to:")
                        .font(.headline)
                    
                    VStack(alignment: .leading, spacing: 8) {
                        Label("View routines", systemImage: "checkmark.circle.fill")
                        Label("Complete tasks", systemImage: "checkmark.circle.fill")
                        Label("See progress", systemImage: "checkmark.circle.fill")
                    }
                    .font(.body)
                    .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.gray.opacity(0.1))
                .cornerRadius(12)
                
                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .padding()
                        .background(Color.red.opacity(0.1))
                        .cornerRadius(8)
                }
                
                HStack(spacing: 16) {
                    Button("Cancel") {
                        onDismiss(false)
                    }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                    
                    Button("Join") {
                        isJoining = true
                        Task {
                            let success = await viewModel.joinFamily()
                            isJoining = false
                            onDismiss(success)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .disabled(isJoining)
                }
            }
            .padding()
            .navigationTitle("Confirm")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

#Preview {
    let dependencies = AppDependencies()
    return ScanInviteView(dependencies: dependencies)
}

