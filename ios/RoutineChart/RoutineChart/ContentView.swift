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
                    .onAppear {
                        AppLogger.log("ContentView: Showing main content. User: \(dependencies.currentAuthUser?.id ?? "unknown")")
                    }
            } else {
                // User is not authenticated - show auth flow
                AuthFlowView()
                    .onAppear {
                        AppLogger.log("ContentView: Showing auth flow. User is nil")
                    }
            }
        }
        .onChange(of: dependencies.currentAuthUser) { newValue in
            AppLogger.log("ContentView: Auth user changed to: \(newValue?.id ?? "nil")")
        }
    }
    
    private var mainContent: some View {
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
    }
}

#Preview {
    ContentView()
        .environmentObject(AppDependencies())
}
