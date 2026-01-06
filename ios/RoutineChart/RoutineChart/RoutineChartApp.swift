//
//  RoutineChartApp.swift
//  RoutineChart
//
//  Created by Christopher Hammers on 1/2/26.
//

import SwiftUI
import FirebaseCore
import OSLog

class AppDelegate: NSObject, UIApplicationDelegate {
    var dependencies: AppDependencies?
    
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()
        return true
    }
}

@main
struct RoutineChartApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var dependencies = AppDependencies()
    @State private var isInitialized = false
    
    var body: some Scene {
        WindowGroup {
            if isInitialized {
                ContentView()
                    .environmentObject(dependencies)
                    .onAppear {
                        delegate.dependencies = dependencies
                    }
            } else {
                ProgressView("Initializing...")
                    .task {
                        await initializeApp()
                    }
            }
        }
    }
    
    private func initializeApp() async {
        do {
            // Initialize database
            try SQLiteManager.shared.setup()
            AppLogger.log("Database initialized")
            
            // Seed data if needed
            try await dependencies.seedDataManager.seedIfNeeded()
            
            isInitialized = true
        } catch {
            AppLogger.error("Failed to initialize app: \(error.localizedDescription)")
            // In production, show error UI
            fatalError("Failed to initialize: \(error)")
        }
    }
}
