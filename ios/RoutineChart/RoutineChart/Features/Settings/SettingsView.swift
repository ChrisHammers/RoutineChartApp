//
//  SettingsView.swift
//
//  Settings view with family management options
//

import SwiftUI
import OSLog

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    let dependencies: AppDependencies
    @State private var showJoinFamily = false
    @State private var isCreatingTestData = false
    @State private var testDataMessage: String?
    
    var body: some View {
        NavigationView {
            List {
                Section("Family") {
                    // Show "Join a Family" for both children and parents
                    // Parents can join other families (e.g., co-parenting scenarios)
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
                
                #if DEBUG
                Section("Testing") {
                    if let user = dependencies.currentUser, user.role == .parent {
                        Button(action: {
                            Task { await createTestChildren() }
                        }) {
                            HStack {
                                Image(systemName: "person.badge.plus")
                                    .foregroundColor(.orange)
                                Text("Create Test Children")
                                if isCreatingTestData {
                                    Spacer()
                                    ProgressView()
                                }
                            }
                        }
                        .disabled(isCreatingTestData)
                        
                        if let message = testDataMessage {
                            Text(message)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                #endif
                
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
    
    #if DEBUG
    private func createTestChildren() async {
        guard let user = dependencies.currentUser,
              user.role == .parent else {
            return
        }
        
        isCreatingTestData = true
        testDataMessage = nil
        
        do {
            let child1 = ChildProfile(
                familyId: user.familyId,
                displayName: "Emma",
                avatarIcon: "🌟",
                ageBand: .age_5_7,
                readingMode: .light_text
            )
            try await dependencies.childProfileRepository.create(child1)
            
            let child2 = ChildProfile(
                familyId: user.familyId,
                displayName: "Noah",
                avatarIcon: "🚀",
                ageBand: .age_8_10,
                readingMode: .full_text
            )
            try await dependencies.childProfileRepository.create(child2)
            
            testDataMessage = "Created Emma and Noah"
            AppLogger.database.info("Created test children for family: \(user.familyId)")
        } catch {
            testDataMessage = "Error: \(error.localizedDescription)"
            AppLogger.error("Failed to create test children: \(error.localizedDescription)")
        }
        
        isCreatingTestData = false
    }
    #endif
}

#Preview {
    SettingsView(dependencies: AppDependencies())
}
