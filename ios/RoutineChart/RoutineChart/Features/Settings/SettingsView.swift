//
//  SettingsView.swift
//  RoutineChart
//
//  Settings view with family management options
//

import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    let dependencies: AppDependencies
    @State private var showJoinFamily = false
    
    var body: some View {
        NavigationView {
            List {
                Section("Family") {
                    // Only show "Join a Family" if user is child
                    if let user = dependencies.currentUser, user.role == .child {
                        Button(action: {
                            showJoinFamily = true
                        }) {
                            HStack {
                                Image(systemName: "person.2.fill")
                                    .foregroundColor(.accentColor)
                                Text("Join a Family")
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                                    .font(.caption)
                            }
                        }
                    }
                }
                
                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showJoinFamily) {
                JoinFamilyOptionsView(dependencies: dependencies)
            }
        }
    }
}

#Preview {
    SettingsView(dependencies: AppDependencies())
}

