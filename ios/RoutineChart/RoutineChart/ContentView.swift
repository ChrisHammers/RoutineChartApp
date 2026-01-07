//
//  ContentView.swift
//  RoutineChart
//
//  Created by Christopher Hammers on 1/2/26.
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var dependencies: AppDependencies
    
    var body: some View {
        Group {
            if dependencies.currentAuthUser != nil {
                // User is authenticated - show main app
                mainContent
                    .task {
                        // Load current user when authenticated
                        await dependencies.loadCurrentUser()
                    }
            } else {
                // User is not authenticated - show auth flow
                AuthFlowView()
            }
        }
    }
    
    private var mainContent: some View {
        Group {
            // If we have a user, show role-based content
            if let user = dependencies.currentUser {
                if user.role == .parent {
                    // Parent sees both tabs
                    TabView {
                        ParentDashboardView(dependencies: dependencies)
                            .tabItem {
                                Label("Parent", systemImage: "person.fill")
                            }
                        
                        ChildTodayView(dependencies: dependencies)
                            .tabItem {
                                Label("Child", systemImage: "figure.child")
                            }
                    }
                } else {
                    // Child only sees child view
                    ChildTodayView(dependencies: dependencies)
                }
            } else {
                // No User record yet - show child view by default
                // This handles anonymous sign-ins who haven't joined a family yet
                // (Anonymous users are treated as children until they join a family)
                ChildTodayView(dependencies: dependencies)
            }
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(AppDependencies())
}
