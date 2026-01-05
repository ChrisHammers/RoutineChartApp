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
            }
            .padding(.horizontal, 24)
            
            Spacer()
        }
        .background(Color(.systemBackground))
    }
}

struct AuthFlowView_Previews: PreviewProvider {
    static var previews: some View {
        AuthFlowView()
            .environmentObject(AppDependencies())
    }
}

