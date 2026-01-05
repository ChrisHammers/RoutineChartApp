//
//  ParentSignInView.swift
//  RoutineChart
//
//  Created for Phase 2.1: Firebase Auth
//

import SwiftUI

struct ParentSignInView: View {
    @StateObject private var viewModel: ParentSignInViewModel
    @FocusState private var focusedField: Field?
    
    enum Field {
        case email, password
    }
    
    init(authRepository: AuthRepository) {
        _viewModel = StateObject(wrappedValue: ParentSignInViewModel(authRepository: authRepository))
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(spacing: 8) {
                        Image(systemName: "person.circle.fill")
                            .font(.system(size: 80))
                            .foregroundColor(.blue)
                        
                        Text(viewModel.isSignUpMode ? "Create Parent Account" : "Parent Sign In")
                            .font(.largeTitle)
                            .fontWeight(.bold)
                        
                        Text("Manage routines and view progress")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 40)
                    
                    // Form
                    VStack(spacing: 16) {
                        // Email
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Email")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                            
                            TextField("you@example.com", text: $viewModel.email)
                                .textFieldStyle(.roundedBorder)
                                .textContentType(.emailAddress)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                                .focused($focusedField, equals: .email)
                                .submitLabel(.next)
                                .onSubmit {
                                    focusedField = .password
                                }
                        }
                        
                        // Password
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Password")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                            
                            SecureField("6+ characters", text: $viewModel.password)
                                .textFieldStyle(.roundedBorder)
                                .textContentType(viewModel.isSignUpMode ? .newPassword : .password)
                                .focused($focusedField, equals: .password)
                                .submitLabel(.go)
                                .onSubmit {
                                    Task {
                                        await viewModel.signIn()
                                    }
                                }
                        }
                        
                        // Error message
                        if let errorMessage = viewModel.errorMessage {
                            Text(errorMessage)
                                .font(.caption)
                                .foregroundColor(errorMessage.contains("sent") ? .green : .red)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        
                        // Sign In/Up Button
                        Button(action: {
                            Task {
                                await viewModel.signIn()
                            }
                        }) {
                            HStack {
                                if viewModel.isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text(viewModel.isSignUpMode ? "Create Account" : "Sign In")
                                        .fontWeight(.semibold)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(viewModel.canSubmit ? Color.blue : Color.gray)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .disabled(!viewModel.canSubmit || viewModel.isLoading)
                        
                        // Toggle Sign Up/Sign In
                        Button(action: viewModel.toggleMode) {
                            Text(viewModel.isSignUpMode ? "Already have an account? Sign In" : "Don't have an account? Sign Up")
                                .font(.subheadline)
                                .foregroundColor(.blue)
                        }
                        .disabled(viewModel.isLoading)
                        
                        // Forgot Password
                        if !viewModel.isSignUpMode {
                            Button(action: {
                                Task {
                                    await viewModel.sendPasswordReset()
                                }
                            }) {
                                Text("Forgot Password?")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .disabled(viewModel.isLoading)
                        }
                    }
                    .padding(.horizontal, 24)
                    
                    Spacer()
                }
            }
            .navigationBarHidden(true)
        }
    }
}

struct ParentSignInView_Previews: PreviewProvider {
    static var previews: some View {
        ParentSignInView(authRepository: FirebaseAuthService())
    }
}

