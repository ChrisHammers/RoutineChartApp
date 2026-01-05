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
            } else {
                // User is not authenticated - show auth flow
                AuthFlowView()
            }
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
