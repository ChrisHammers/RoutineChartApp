//
//  GenerateInviteView.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import SwiftUI
import UniformTypeIdentifiers

// Make UIImage shareable with ShareLink
extension UIImage: Transferable {
    public static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .png) { image in
            image.pngData() ?? Data()
        }
    }
}

struct GenerateInviteView: View {
    @StateObject private var viewModel: GenerateInviteViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showCopyConfirmation = false
    
    init(dependencies: AppDependencies) {
        _viewModel = StateObject(
            wrappedValue: GenerateInviteViewModel(
                inviteRepository: dependencies.familyInviteRepository,
                familyRepository: dependencies.familyRepository,
                authRepository: dependencies.authRepository
            )
        )
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    if viewModel.isLoading {
                        ProgressView(viewModel.loadingMessage)
                            .padding()
                    } else if let qrImage = viewModel.qrCodeImage, let invite = viewModel.invite {
                        // QR Code Display
                        VStack(spacing: 16) {
                            Text("Invite Code")
                                .font(.title2)
                                .fontWeight(.semibold)
                            
                            // Large, copyable invite code
                            VStack(spacing: 8) {
                                Text(invite.inviteCode)
                                    .font(.system(size: 48, weight: .bold, design: .monospaced))
                                    .foregroundColor(.primary)
                                    .padding()
                                    .background(Color.gray.opacity(0.1))
                                    .cornerRadius(12)
                                    .onTapGesture {
                                        UIPasteboard.general.string = invite.inviteCode
                                        withAnimation {
                                            showCopyConfirmation = true
                                        }
                                        // Hide confirmation after 2 seconds
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                                            withAnimation {
                                                showCopyConfirmation = false
                                            }
                                        }
                                    }
                                
                                if showCopyConfirmation {
                                    HStack(spacing: 4) {
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundColor(.green)
                                        Text("Copied!")
                                            .font(.caption)
                                            .foregroundColor(.green)
                                    }
                                    .transition(.opacity.combined(with: .scale))
                                } else {
                                    VStack(spacing: 4) {
                                        Text("Tap code to copy • Share with others")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                        
                                        // Real-time usage count
                                        if invite.usedCount > 0 {
                                            Text("\(invite.usedCount) \(invite.usedCount == 1 ? "person has" : "people have") joined")
                                                .font(.caption)
                                                .foregroundColor(.green)
                                                .fontWeight(.medium)
                                        }
                                    }
                                    .transition(.opacity)
                                }
                            }
                            
                            Divider()
                                .padding(.vertical, 8)
                            
                            Text("Or Scan QR Code")
                                .font(.headline)
                                .foregroundColor(.secondary)
                            
                            Image(uiImage: qrImage)
                                .interpolation(.none)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 250, height: 250)
                                .background(Color.white)
                                .cornerRadius(12)
                                .shadow(radius: 4)
                            
                            Text(viewModel.timeRemaining)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            // Actions
                            HStack(spacing: 16) {
                                if let shareableImage = viewModel.shareableImage(for: invite, qrImage: qrImage) {
                                    ShareLink(
                                        item: shareableImage,
                                        preview: SharePreview(
                                            "Join my family on Routine Chart",
                                            image: Image(uiImage: shareableImage)
                                        )
                                    ) {
                                        Label("Share", systemImage: "square.and.arrow.up")
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.bordered)
                                }
                                
                                Button(role: .destructive, action: {
                                    Task {
                                        await viewModel.deactivateInvite()
                                    }
                                }) {
                                    Label("Deactivate", systemImage: "xmark.circle")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.bordered)
                            }
                            .padding(.top, 8)
                        }
                        .padding()
                    } else {
                        // Initial State
                        VStack(spacing: 16) {
                            Image(systemName: "qrcode")
                                .font(.system(size: 80))
                                .foregroundColor(.accentColor)
                            
                            Text("Invite Family Member")
                                .font(.title2)
                                .fontWeight(.semibold)
                            
                            Text("Generate a QR code that others can scan to join your family")
                                .font(.body)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                            
                            Button(action: {
                                Task {
                                    await viewModel.generateInvite()
                                }
                            }) {
                                Label("Generate QR Code", systemImage: "qrcode")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .padding(.horizontal)
                        }
                        .padding()
                    }
                    
                    // Error Message
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
            .navigationTitle("Invite Member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .task {
                // Load existing active invite when view appears
                await viewModel.loadActiveInvite()
            }
        }
    }
}

#Preview {
    let dependencies = AppDependencies()
    return GenerateInviteView(dependencies: dependencies)
}

