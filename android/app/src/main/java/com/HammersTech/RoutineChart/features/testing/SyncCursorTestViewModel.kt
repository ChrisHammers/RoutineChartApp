package com.HammersTech.RoutineChart.features.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.data.remote.firebase.SyncCursorManager
import com.HammersTech.RoutineChart.core.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for SyncCursor CRUD testing
 * Phase 3.1: Sync Infrastructure
 */
@HiltViewModel
class SyncCursorTestViewModel @Inject constructor(
    private val cursorManager: SyncCursorManager
) : ViewModel() {
    
    private val _testResults = MutableStateFlow<List<String>>(emptyList())
    val testResults: StateFlow<List<String>> = _testResults.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val testCollection = "test_routines"
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    fun runTests() {
        viewModelScope.launch {
            _isRunning.value = true
            clearResults()
            addResult("🧪 Starting SyncCursor CRUD Tests...")
            addResult("")
            
            // Run all tests sequentially
            // Test 1: Verify empty
            addResult("📋 Test 1: Verify Initial State (Empty)")
            try {
                val cursors = cursorManager.getAllCursors()
                val count = cursors.size
                if (count == 0) {
                    addResult("   ✅ PASS: Database is empty (0 cursors)")
                } else {
                    addResult("   ⚠️ WARNING: Database has $count cursor(s) (expected: 0)")
                    cursors.forEach { cursor ->
                        addResult("      - ${cursor.collection}: ${cursor.lastSyncedAt}")
                    }
                }
            } catch (e: Exception) {
                addResult("   ❌ FAIL: ${e.message}")
            }
            addResult("")
            
            // Test 2: Create cursor
            addResult("📋 Test 2: Create Cursor")
            val testDate = Instant.now()
            try {
                cursorManager.updateCursor(testCollection, testDate)
                addResult("   ✅ PASS: Created cursor for '$testCollection'")
                addResult("      Timestamp: $testDate")
            } catch (e: Exception) {
                addResult("   ❌ FAIL: ${e.message}")
            }
            addResult("")
            
            // Test 3: Read cursor
            addResult("📋 Test 3: Read Cursor")
            try {
                val cursor = cursorManager.getCursor(testCollection)
                if (cursor != null) {
                    addResult("   ✅ PASS: Read cursor successfully")
                    addResult("      Collection: ${cursor.collection}")
                    addResult("      Timestamp: ${cursor.lastSyncedAt}")
                } else {
                    addResult("   ❌ FAIL: Cursor not found")
                }
            } catch (e: Exception) {
                addResult("   ❌ FAIL: ${e.message}")
            }
            addResult("")
            
            // Test 4: Update cursor
            addResult("📋 Test 4: Update Cursor")
            val newDate = Instant.now().plusSeconds(3600) // 1 hour later
            try {
                cursorManager.updateCursor(testCollection, newDate)
                // Small delay to ensure database write completes
                kotlinx.coroutines.delay(100)
                val updated = cursorManager.getCursor(testCollection)
                if (updated != null) {
                    // Compare timestamps with 1 second tolerance (SQLite stores as milliseconds, so nanoseconds are lost)
                    val timeDiff = kotlin.math.abs(java.time.Duration.between(updated.lastSyncedAt, newDate).seconds)
                    if (timeDiff < 1) {
                        addResult("   ✅ PASS: Updated cursor successfully")
                        addResult("      New timestamp: ${updated.lastSyncedAt}")
                    } else {
                        addResult("   ⚠️ WARNING: Update completed but timestamp mismatch")
                        addResult("      Expected: $newDate")
                        addResult("      Got: ${updated.lastSyncedAt}")
                        addResult("      Difference: ${timeDiff}s")
                    }
                } else {
                    addResult("   ❌ FAIL: Cursor not found after update")
                }
            } catch (e: Exception) {
                addResult("   ❌ FAIL: ${e.message}")
            }
            addResult("")
            
            // Test 5: Get all cursors
            addResult("📋 Test 5: Get All Cursors")
            try {
                val all = cursorManager.getAllCursors()
                addResult("   ✅ PASS: Retrieved ${all.size} cursor(s)")
                if (all.isEmpty()) {
                    addResult("      (No cursors found)")
                } else {
                    all.forEach { cursor ->
                        addResult("      - ${cursor.collection}: ${cursor.lastSyncedAt}")
                    }
                }
            } catch (e: Exception) {
                addResult("   ❌ FAIL: ${e.message}")
            }
            addResult("")
            
            // Test 6: Delete cursor
            addResult("📋 Test 6: Delete Cursor")
            try {
                cursorManager.deleteCursor(testCollection)
                // Small delay to ensure database write completes
                kotlinx.coroutines.delay(100)
                val deleted = cursorManager.getCursor(testCollection)
                if (deleted == null) {
                    addResult("   ✅ PASS: Deleted cursor successfully")
                } else {
                    addResult("   ❌ FAIL: Cursor still exists after delete")
                }
            } catch (e: Exception) {
                addResult("   ❌ FAIL: ${e.message}")
            }
            addResult("")
            
            // Test 7: Verify empty again
            addResult("📋 Test 7: Verify Final State (Empty)")
            try {
                val cursors = cursorManager.getAllCursors()
                val count = cursors.size
                if (count == 0) {
                    addResult("   ✅ PASS: Database is empty (0 cursors)")
                } else {
                    addResult("   ⚠️ WARNING: Database has $count cursor(s) (expected: 0)")
                }
            } catch (e: Exception) {
                addResult("   ❌ FAIL: ${e.message}")
            }
            addResult("")
            
            addResult("🎉 All tests completed!")
            _isRunning.value = false
        }
    }
    
    fun clearResults() {
        _testResults.value = emptyList()
    }
    
    private fun addResult(message: String) {
        _testResults.value = _testResults.value + message
        AppLogger.Database.info("[SyncCursorTest] $message")
    }
}
