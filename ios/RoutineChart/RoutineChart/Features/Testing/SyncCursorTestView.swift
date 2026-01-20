//
//  SyncCursorTestView.swift
//  RoutineChart
//
//  Created for Phase 3.1: SyncCursor Testing
//  Simple test screen to verify SyncCursor CRUD operations
//

import SwiftUI
import Combine

struct SyncCursorTestView: View {
    @StateObject private var viewModel = SyncCursorTestViewModel()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 8) {
                    Text("SyncCursor CRUD Test")
                        .font(.title)
                        .fontWeight(.bold)
                    
                    Text("Phase 3.1: Sync Infrastructure")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top)
                
                // Test Results
                ScrollView {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(viewModel.testResults, id: \.self) { result in
                            Text(result)
                                .font(.system(.body, design: .monospaced))
                                .padding(.horizontal)
                        }
                    }
                    .padding(.vertical)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemGray6))
                .cornerRadius(12)
                
                // Test Button
                Button(action: {
                    Task {
                        await viewModel.runTests()
                    }
                }) {
                    HStack {
                        if viewModel.isRunning {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        }
                        Text(viewModel.isRunning ? "Running Tests..." : "Run All Tests")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(viewModel.isRunning ? Color.gray : Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(viewModel.isRunning)
                
                // Individual Test Buttons
                VStack(spacing: 12) {
                    Text("Individual Tests")
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    HStack(spacing: 12) {
                        Button("Create") {
                            Task {
                                await viewModel.testCreate()
                            }
                        }
                        .buttonStyle(TestButtonStyle())
                        
                        Button("Read") {
                            Task {
                                await viewModel.testRead()
                            }
                        }
                        .buttonStyle(TestButtonStyle())
                        
                        Button("Update") {
                            Task {
                                await viewModel.testUpdate()
                            }
                        }
                        .buttonStyle(TestButtonStyle())
                    }
                    
                    HStack(spacing: 12) {
                        Button("Get All") {
                            Task {
                                await viewModel.testGetAll()
                            }
                        }
                        .buttonStyle(TestButtonStyle())
                        
                        Button("Delete") {
                            Task {
                                await viewModel.testDelete()
                            }
                        }
                        .buttonStyle(TestButtonStyle())
                        
                        Button("Clear") {
                            viewModel.clearResults()
                        }
                        .buttonStyle(TestButtonStyle(backgroundColor: .orange))
                    }
                }
                
                Spacer()
            }
            .padding()
            .navigationTitle("SyncCursor Test")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Test Button Style

struct TestButtonStyle: ButtonStyle {
    var backgroundColor: Color = .blue
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(.body, design: .monospaced))
            .foregroundColor(.white)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(backgroundColor.opacity(configuration.isPressed ? 0.7 : 1.0))
            .cornerRadius(8)
    }
}

// MARK: - ViewModel

@MainActor
final class SyncCursorTestViewModel: ObservableObject {
    @Published var testResults: [String] = []
    @Published var isRunning = false
    
    private let cursorManager = SyncCursorManager()
    private let testCollection = "test_routines"
    
    func runTests() async {
        isRunning = true
        clearResults()
        addResult("🧪 Starting SyncCursor CRUD Tests...")
        addResult("")
        
        // Test 1: Verify empty
        await testInitialState()
        
        // Test 2: Create cursor
        await testCreate()
        
        // Test 3: Read cursor
        await testRead()
        
        // Test 4: Update cursor
        await testUpdate()
        
        // Test 5: Get all cursors
        await testGetAll()
        
        // Test 6: Delete cursor
        await testDelete()
        
        // Test 7: Verify empty again
        await testFinalState()
        
        addResult("")
        addResult("🎉 All tests completed!")
        isRunning = false
    }
    
    func testInitialState() async {
        addResult("📋 Test 1: Verify Initial State (Empty)")
        do {
            let cursors = try await cursorManager.getAllCursors()
            let count = cursors.count
            if count == 0 {
                addResult("   ✅ PASS: Database is empty (0 cursors)")
            } else {
                addResult("   ⚠️ WARNING: Database has \(count) cursor(s) (expected: 0)")
                cursors.forEach { cursor in
                    addResult("      - \(cursor.collection): \(cursor.lastSyncedAt)")
                }
            }
        } catch {
            addResult("   ❌ FAIL: \(error.localizedDescription)")
        }
        addResult("")
    }
    
    func testCreate() async {
        addResult("📋 Test 2: Create Cursor")
        let testDate = Date()
        do {
            try await cursorManager.updateCursor(collection: testCollection, lastSyncedAt: testDate)
            addResult("   ✅ PASS: Created cursor for '\(testCollection)'")
            addResult("      Timestamp: \(testDate.formatted())")
        } catch {
            addResult("   ❌ FAIL: \(error.localizedDescription)")
        }
        addResult("")
    }
    
    func testRead() async {
        addResult("📋 Test 3: Read Cursor")
        do {
            let cursor = try await cursorManager.getCursor(collection: testCollection)
            if let cursor = cursor {
                addResult("   ✅ PASS: Read cursor successfully")
                addResult("      Collection: \(cursor.collection)")
                addResult("      Timestamp: \(cursor.lastSyncedAt.formatted())")
            } else {
                addResult("   ❌ FAIL: Cursor not found")
            }
        } catch {
            addResult("   ❌ FAIL: \(error.localizedDescription)")
        }
        addResult("")
    }
    
    func testUpdate() async {
        addResult("📋 Test 4: Update Cursor")
        let newDate = Date().addingTimeInterval(3600) // 1 hour later
        do {
            try await cursorManager.updateCursor(collection: testCollection, lastSyncedAt: newDate)
            let updated = try await cursorManager.getCursor(collection: testCollection)
            if let updated = updated {
                if updated.lastSyncedAt == newDate {
                    addResult("   ✅ PASS: Updated cursor successfully")
                    addResult("      New timestamp: \(updated.lastSyncedAt.formatted())")
                } else {
                    addResult("   ⚠️ WARNING: Update completed but timestamp mismatch")
                    addResult("      Expected: \(newDate.formatted())")
                    addResult("      Got: \(updated.lastSyncedAt.formatted())")
                }
            } else {
                addResult("   ❌ FAIL: Cursor not found after update")
            }
        } catch {
            addResult("   ❌ FAIL: \(error.localizedDescription)")
        }
        addResult("")
    }
    
    func testGetAll() async {
        addResult("📋 Test 5: Get All Cursors")
        do {
            let all = try await cursorManager.getAllCursors()
            addResult("   ✅ PASS: Retrieved \(all.count) cursor(s)")
            if all.isEmpty {
                addResult("      (No cursors found)")
            } else {
                all.forEach { cursor in
                    addResult("      - \(cursor.collection): \(cursor.lastSyncedAt.formatted())")
                }
            }
        } catch {
            addResult("   ❌ FAIL: \(error.localizedDescription)")
        }
        addResult("")
    }
    
    func testDelete() async {
        addResult("📋 Test 6: Delete Cursor")
        do {
            try await cursorManager.deleteCursor(collection: testCollection)
            let deleted = try await cursorManager.getCursor(collection: testCollection)
            if deleted == nil {
                addResult("   ✅ PASS: Deleted cursor successfully")
            } else {
                addResult("   ❌ FAIL: Cursor still exists after delete")
            }
        } catch {
            addResult("   ❌ FAIL: \(error.localizedDescription)")
        }
        addResult("")
    }
    
    func testFinalState() async {
        addResult("📋 Test 7: Verify Final State (Empty)")
        do {
            let cursors = try await cursorManager.getAllCursors()
            let count = cursors.count
            if count == 0 {
                addResult("   ✅ PASS: Database is empty (0 cursors)")
            } else {
                addResult("   ⚠️ WARNING: Database has \(count) cursor(s) (expected: 0)")
            }
        } catch {
            addResult("   ❌ FAIL: \(error.localizedDescription)")
        }
        addResult("")
    }
    
    func clearResults() {
        testResults = []
    }
    
    private func addResult(_ message: String) {
        testResults.append(message)
        print("[SyncCursorTest] \(message)") // Also log to console
    }
}

// MARK: - Preview

#Preview {
    SyncCursorTestView()
}
