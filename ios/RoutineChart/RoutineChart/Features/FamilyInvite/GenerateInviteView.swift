//
//  GenerateInviteView.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import SwiftUI

struct GenerateInviteView: View {
    @StateObject private var viewModel: GenerateInviteViewModel
    @Environment(\.dismiss) private var dismiss
    
    init(dependencies: AppDependencies) {
        _viewModel = StateObject(
            wrappedValue: GenerateInviteViewModel(
                inviteRepository: dependencies.familyInviteRepository,
                familyRepository: dependencies.familyRepository
            )
        )
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    if viewModel.isLoading {
                        ProgressView("Generating invite...")
                            .padding()
                    } else if let qrImage = viewModel.qrCodeImage, let invite = viewModel.invite {
                        // QR Code Display
                        VStack(spacing: 16) {
                            Text("Invite Code")
                                .font(.title2)
                                .fontWeight(.semibold)
                            
                            // Large, copyable invite code
                            Text(invite.inviteCode)
                                .font(.system(size: 48, weight: .bold, design: .monospaced))
                                .foregroundColor(.primary)
                                .padding()
                                .background(Color.gray.opacity(0.1))
                                .cornerRadius(12)
                                .onTapGesture {
                                    UIPasteboard.general.string = invite.inviteCode
                                }
                            
                            Text("Tap code to copy • Share with others")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
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
                                Button(action: { viewModel.shareInvite() }) {
                                    Label("Share", systemImage: "square.and.arrow.up")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.bordered)
                                
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
        }
    }
}

#Preview {
    let dependencies = AppDependencies()
    return GenerateInviteView(dependencies: dependencies)
}

