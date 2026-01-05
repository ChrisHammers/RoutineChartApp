//
//  ChildSignInView.swift
//  RoutineChart
//
//  Created for Phase 2.1: Firebase Auth
//

import SwiftUI

struct ChildSignInView: View {
    @StateObject private var viewModel: ChildSignInViewModel
    
    init(authRepository: AuthRepository) {
        _viewModel = StateObject(wrappedValue: ChildSignInViewModel(authRepository: authRepository))
    }
    
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            // Header
            VStack(spacing: 16) {
                Image(systemName: "figure.child")
                    .font(.system(size: 100))
                    .foregroundColor(.green)
                
                Text("Welcome!")
                    .font(.system(size: 48, weight: .bold))
                
                Text("Tap to start your routines")
                    .font(.title3)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Error message
            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(.body)
                    .foregroundColor(.red)
                    .padding()
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(12)
                    .padding(.horizontal, 24)
            }
            
            // Big Start Button
            Button(action: {
                Task {
                    await viewModel.signInAsChild()
                }
            }) {
                HStack(spacing: 16) {
                    if viewModel.isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(1.5)
                    } else {
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 40))
                        Text("Start")
                            .font(.system(size: 32, weight: .bold))
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
                .background(Color.green)
                .foregroundColor(.white)
                .cornerRadius(20)
                .shadow(radius: 4)
            }
            .disabled(viewModel.isLoading)
            .padding(.horizontal, 40)
            
            Spacer()
        }
        .background(Color(.systemBackground))
    }
}

struct ChildSignInView_Previews: PreviewProvider {
    static var previews: some View {
        ChildSignInView(authRepository: FirebaseAuthService())
    }
}

