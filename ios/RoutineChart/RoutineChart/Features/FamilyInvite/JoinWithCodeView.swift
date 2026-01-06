//
//  JoinWithCodeView.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining (Manual Code Entry)
//

import SwiftUI

struct JoinWithCodeView: View {
    @StateObject private var viewModel: JoinWithCodeViewModel
    @Environment(\.dismiss) private var dismiss
    @FocusState private var isCodeFieldFocused: Bool
    
    init(dependencies: AppDependencies) {
        _viewModel = StateObject(
            wrappedValue: JoinWithCodeViewModel(
                inviteRepository: dependencies.familyInviteRepository,
                familyRepository: dependencies.familyRepository,
                userRepository: dependencies.userRepository
            )
        )
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Spacer()
                
                // Icon
                Image(systemName: "keyboard")
                    .font(.system(size: 64))
                    .foregroundColor(.accentColor)
                
                // Title
                Text("Enter Invite Code")
                    .font(.title)
                    .fontWeight(.bold)
                
                // Description
                Text("Enter the code shared by your family member")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                
                // Code Input
                VStack(spacing: 8) {
                    TextField("ABC-1234", text: $viewModel.inviteCode)
                        .textFieldStyle(.roundedBorder)
                        .font(.system(size: 32, weight: .bold, design: .monospaced))
                        .multilineTextAlignment(.center)
                        .textInputAutocapitalization(.characters)
                        .focused($isCodeFieldFocused)
                        .disabled(viewModel.isJoining)
                    
                    Text("Format: XXX-YYYY (e.g., ABC-1234)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal)
                
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
                
                // Join Button
                Button(action: {
                    Task {
                        await viewModel.joinWithCode()
                    }
                }) {
                    if viewModel.isJoining {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .frame(maxWidth: .infinity)
                    } else {
                        Text("Join Family")
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .padding(.horizontal)
                .disabled(!viewModel.isCodeValid || viewModel.isJoining)
                
                Spacer()
            }
            .padding()
            .navigationTitle("Join with Code")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .disabled(viewModel.isJoining)
                }
            }
            .onAppear {
                isCodeFieldFocused = true
            }
            .alert("Success!", isPresented: $viewModel.joinSuccess) {
                Button("OK") {
                    dismiss()
                }
            } message: {
                Text("You've successfully joined the family!")
            }
        }
    }
}

#Preview {
    let dependencies = AppDependencies()
    return JoinWithCodeView(dependencies: dependencies)
}

