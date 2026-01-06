//
//  AuthFlowView.swift
//  RoutineChart
//
//  Created for Phase 2.1: Firebase Auth
//

import SwiftUI

struct AuthFlowView: View {
    @EnvironmentObject var dependencies: AppDependencies
    @State private var selectedMode: AuthMode?
    @State private var showJoinFamily = false
    
    enum AuthMode {
        case parent, child
    }
    
    var body: some View {
        Group {
            if let mode = selectedMode {
                // Show selected sign-in view
                switch mode {
                case .parent:
                    ParentSignInView(
                        authRepository: dependencies.authRepository,
                        onBack: { selectedMode = nil }
                    )
                    .transition(.move(edge: .trailing))
                case .child:
                    ChildSignInView(
                        authRepository: dependencies.authRepository,
                        onBack: { selectedMode = nil }
                    )
                    .transition(.move(edge: .trailing))
                }
            } else {
                // Show mode selection
                modeSelectionView
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut, value: selectedMode)
    }
    
    private var modeSelectionView: some View {
        VStack(spacing: 40) {
            // Header
            VStack(spacing: 16) {
                Text("🏠")
                    .font(.system(size: 80))
                
                Text("Routine Chart")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Text("Who are you?")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            .padding(.top, 60)
            
            Spacer()
            
            // Mode Selection Cards
            VStack(spacing: 24) {
                // Parent Button
                Button(action: {
                    selectedMode = .parent
                }) {
                    HStack(spacing: 20) {
                        Image(systemName: "person.circle.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.blue)
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("I'm a Parent")
                                .font(.title2)
                                .fontWeight(.semibold)
                            Text("Manage routines & track progress")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        Image(systemName: "chevron.right")
                            .font(.title3)
                            .foregroundColor(.secondary)
                    }
                    .padding(24)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(16)
                }
                .buttonStyle(PlainButtonStyle())
                
                // Child Button
                Button(action: {
                    selectedMode = .child
                }) {
                    HStack(spacing: 20) {
                        Image(systemName: "figure.child")
                            .font(.system(size: 60))
                            .foregroundColor(.green)
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("I'm a Child")
                                .font(.title2)
                                .fontWeight(.semibold)
                            Text("Complete my routines")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        Image(systemName: "chevron.right")
                            .font(.title3)
                            .foregroundColor(.secondary)
                    }
                    .padding(24)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(16)
                }
                .buttonStyle(PlainButtonStyle())
                
                // Join Family Button
                Button(action: {
                    showJoinFamily = true
                }) {
                    HStack(spacing: 20) {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 60))
                            .foregroundColor(.purple)
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Join a Family")
                                .font(.title2)
                                .fontWeight(.semibold)
                            Text("Scan QR code or enter invite code")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        Image(systemName: "chevron.right")
                            .font(.title3)
                            .foregroundColor(.secondary)
                    }
                    .padding(24)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(16)
                }
                .buttonStyle(PlainButtonStyle())
            }
            .padding(.horizontal, 24)
            
            Spacer()
        }
        .background(Color(.systemBackground))
        .sheet(isPresented: $showJoinFamily) {
            JoinFamilyOptionsView(dependencies: dependencies)
        }
    }
}

// MARK: - Join Family Options View

struct JoinFamilyOptionsView: View {
    @Environment(\.dismiss) private var dismiss
    let dependencies: AppDependencies
    @State private var showScanQR = false
    @State private var showEnterCode = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Spacer()
                
                Image(systemName: "person.2.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.accentColor)
                
                Text("Join a Family")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text("Choose how you'd like to join")
                    .font(.body)
                    .foregroundColor(.secondary)
                
                VStack(spacing: 16) {
                    // Scan QR Code Button
                    Button(action: {
                        showScanQR = true
                    }) {
                        HStack(spacing: 16) {
                            Image(systemName: "qrcode.viewfinder")
                                .font(.title)
                                .foregroundColor(.white)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Scan QR Code")
                                    .font(.headline)
                                    .foregroundColor(.white)
                                Text("Use your camera to scan")
                                    .font(.subheadline)
                                    .foregroundColor(.white.opacity(0.9))
                            }
                            
                            Spacer()
                            
                            Image(systemName: "chevron.right")
                                .foregroundColor(.white.opacity(0.7))
                        }
                        .padding(20)
                        .background(Color.accentColor)
                        .cornerRadius(12)
                    }
                    
                    // Enter Code Button
                    Button(action: {
                        showEnterCode = true
                    }) {
                        HStack(spacing: 16) {
                            Image(systemName: "textformat.123")
                                .font(.title)
                                .foregroundColor(.white)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Enter Invite Code")
                                    .font(.headline)
                                    .foregroundColor(.white)
                                Text("Type the code manually")
                                    .font(.subheadline)
                                    .foregroundColor(.white.opacity(0.9))
                            }
                            
                            Spacer()
                            
                            Image(systemName: "chevron.right")
                                .foregroundColor(.white.opacity(0.7))
                        }
                        .padding(20)
                        .background(Color.purple)
                        .cornerRadius(12)
                    }
                }
                .padding(.horizontal, 24)
                
                Spacer()
            }
            .padding()
            .navigationTitle("Join Family")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showScanQR) {
                ScanInviteView(dependencies: dependencies)
            }
            .sheet(isPresented: $showEnterCode) {
                JoinWithCodeView(dependencies: dependencies)
            }
        }
    }
}

struct AuthFlowView_Previews: PreviewProvider {
    static var previews: some View {
        AuthFlowView()
            .environmentObject(AppDependencies())
    }
}

