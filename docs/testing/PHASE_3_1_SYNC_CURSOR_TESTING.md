# Phase 3.1: SyncCursor Testing Guide

## Overview
This guide helps you test the SyncCursor CRUD operations locally (Phase 1) before cloud sync is implemented.

---

## ✅ Test 1: Verify Database is Clear (Initial State)

### iOS (SQLite)
```sql
-- Connect to the database file (usually in app's Documents directory)
-- Or use Xcode's Database Inspector if available

-- Check if sync_cursors table exists and is empty
SELECT COUNT(*) FROM sync_cursors;
-- Expected: 0 (no cursors initially)
```

**Via Terminal (if you have sqlite3):**
```bash
# Find the database file (usually in ~/Library/Developer/CoreSimulator/Devices/[DEVICE_ID]/data/Containers/Data/Application/[APP_ID]/Documents/)
# Then:
sqlite3 routine_chart.db "SELECT COUNT(*) FROM sync_cursors;"
```

### Android (Room)
**Method 1: Database Inspector (Easiest)**
1. In Android Studio: **View → Tool Windows → App Inspection**
2. Select your device/emulator
3. Go to **Database Inspector** tab
4. Select `routine_chart.db`
5. Open `sync_cursors` table
6. **Expected:** Table exists but is empty (0 rows)

**Method 2: Via Logcat**
Check logs for: `"Database schema v4 created - added sync_cursors table"`

**Method 3: SQL Query (if you have adb shell access)**
```bash
adb shell
run-as com.HammersTech.RoutineChart
cd databases
sqlite3 routine_chart.db "SELECT COUNT(*) FROM sync_cursors;"
```

---

## ✅ Test 2: Create (Insert) SyncCursor

### iOS Test Code
Add this to your app (e.g., in `RoutineChartApp.swift` or a test view):

```swift
import SwiftUI

struct SyncCursorTestView: View {
    @State private var testResults: [String] = []
    private let cursorManager = SyncCursorManager()
    
    var body: some View {
        VStack(spacing: 20) {
            Text("SyncCursor CRUD Test")
                .font(.title)
            
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(testResults, id: \.self) { result in
                        Text(result)
                            .font(.system(.body, design: .monospaced))
                    }
                }
            }
            
            Button("Run Tests") {
                Task {
                    await runTests()
                }
            }
        }
        .padding()
    }
    
    func runTests() async {
        testResults = []
        addResult("🧪 Starting SyncCursor CRUD Tests...")
        
        // Test 1: Verify empty
        do {
            let count = try await cursorManager.getAllCursors().count
            addResult("✅ Test 1: Initial count = \(count) (expected: 0)")
        } catch {
            addResult("❌ Test 1 failed: \(error)")
        }
        
        // Test 2: Create cursor
        let testCollection = "test_routines"
        let testDate = Date()
        do {
            try await cursorManager.updateCursor(collection: testCollection, lastSyncedAt: testDate)
            addResult("✅ Test 2: Created cursor for '\(testCollection)'")
        } catch {
            addResult("❌ Test 2 failed: \(error)")
        }
        
        // Test 3: Read cursor
        do {
            let cursor = try await cursorManager.getCursor(collection: testCollection)
            if let cursor = cursor {
                addResult("✅ Test 3: Read cursor - collection: \(cursor.collection), date: \(cursor.lastSyncedAt)")
            } else {
                addResult("❌ Test 3: Cursor not found")
            }
        } catch {
            addResult("❌ Test 3 failed: \(error)")
        }
        
        // Test 4: Update cursor
        let newDate = Date().addingTimeInterval(3600)
        do {
            try await cursorManager.updateCursor(collection: testCollection, lastSyncedAt: newDate)
            let updated = try await cursorManager.getCursor(collection: testCollection)
            if let updated = updated, updated.lastSyncedAt == newDate {
                addResult("✅ Test 4: Updated cursor - new date: \(updated.lastSyncedAt)")
            } else {
                addResult("❌ Test 4: Update failed or date mismatch")
            }
        } catch {
            addResult("❌ Test 4 failed: \(error)")
        }
        
        // Test 5: Get all cursors
        do {
            let all = try await cursorManager.getAllCursors()
            addResult("✅ Test 5: GetAll returned \(all.count) cursor(s)")
            for cursor in all {
                addResult("   - \(cursor.collection): \(cursor.lastSyncedAt)")
            }
        } catch {
            addResult("❌ Test 5 failed: \(error)")
        }
        
        // Test 6: Delete cursor
        do {
            try await cursorManager.deleteCursor(collection: testCollection)
            let deleted = try await cursorManager.getCursor(collection: testCollection)
            if deleted == nil {
                addResult("✅ Test 6: Deleted cursor successfully")
            } else {
                addResult("❌ Test 6: Cursor still exists after delete")
            }
        } catch {
            addResult("❌ Test 6 failed: \(error)")
        }
        
        // Test 7: Verify empty again
        do {
            let count = try await cursorManager.getAllCursors().count
            addResult("✅ Test 7: Final count = \(count) (expected: 0)")
        } catch {
            addResult("❌ Test 7 failed: \(error)")
        }
        
        addResult("🎉 All tests completed!")
    }
    
    func addResult(_ message: String) {
        testResults.append(message)
        print(message) // Also log to console
    }
}
```

### Android Test Code
Add this to your app (e.g., create a test Activity/Composable):

```kotlin
@Composable
fun SyncCursorTestScreen(
    cursorManager: SyncCursorManager = hiltViewModel<SyncCursorTestViewModel>().cursorManager
) {
    val viewModel: SyncCursorTestViewModel = hiltViewModel()
    val testResults by viewModel.testResults.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SyncCursor CRUD Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        LazyColumn {
            items(testResults) { result ->
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Button(onClick = { viewModel.runTests() }) {
            Text("Run Tests")
        }
    }
}

@HiltViewModel
class SyncCursorTestViewModel @Inject constructor(
    val cursorManager: SyncCursorManager
) : ViewModel() {
    private val _testResults = MutableStateFlow<List<String>>(emptyList())
    val testResults: StateFlow<List<String>> = _testResults
    
    fun runTests() {
        viewModelScope.launch {
            _testResults.value = emptyList()
            addResult("🧪 Starting SyncCursor CRUD Tests...")
            
            // Test 1: Verify empty
            try {
                val count = cursorManager.getAllCursors().size
                addResult("✅ Test 1: Initial count = $count (expected: 0)")
            } catch (e: Exception) {
                addResult("❌ Test 1 failed: ${e.message}")
            }
            
            // Test 2: Create cursor
            val testCollection = "test_routines"
            val testDate = Instant.now()
            try {
                cursorManager.updateCursor(testCollection, testDate)
                addResult("✅ Test 2: Created cursor for '$testCollection'")
            } catch (e: Exception) {
                addResult("❌ Test 2 failed: ${e.message}")
            }
            
            // Test 3: Read cursor
            try {
                val cursor = cursorManager.getCursor(testCollection)
                if (cursor != null) {
                    addResult("✅ Test 3: Read cursor - collection: ${cursor.collection}, date: ${cursor.lastSyncedAt}")
                } else {
                    addResult("❌ Test 3: Cursor not found")
                }
            } catch (e: Exception) {
                addResult("❌ Test 3 failed: ${e.message}")
            }
            
            // Test 4: Update cursor
            val newDate = Instant.now().plusSeconds(3600)
            try {
                cursorManager.updateCursor(testCollection, newDate)
                val updated = cursorManager.getCursor(testCollection)
                if (updated != null && updated.lastSyncedAt == newDate) {
                    addResult("✅ Test 4: Updated cursor - new date: ${updated.lastSyncedAt}")
                } else {
                    addResult("❌ Test 4: Update failed or date mismatch")
                }
            } catch (e: Exception) {
                addResult("❌ Test 4 failed: ${e.message}")
            }
            
            // Test 5: Get all cursors
            try {
                val all = cursorManager.getAllCursors()
                addResult("✅ Test 5: GetAll returned ${all.size} cursor(s)")
                all.forEach { cursor ->
                    addResult("   - ${cursor.collection}: ${cursor.lastSyncedAt}")
                }
            } catch (e: Exception) {
                addResult("❌ Test 5 failed: ${e.message}")
            }
            
            // Test 6: Delete cursor
            try {
                cursorManager.deleteCursor(testCollection)
                val deleted = cursorManager.getCursor(testCollection)
                if (deleted == null) {
                    addResult("✅ Test 6: Deleted cursor successfully")
                } else {
                    addResult("❌ Test 6: Cursor still exists after delete")
                }
            } catch (e: Exception) {
                addResult("❌ Test 6 failed: ${e.message}")
            }
            
            // Test 7: Verify empty again
            try {
                val count = cursorManager.getAllCursors().size
                addResult("✅ Test 7: Final count = $count (expected: 0)")
            } catch (e: Exception) {
                addResult("❌ Test 7 failed: ${e.message}")
            }
            
            addResult("🎉 All tests completed!")
        }
    }
    
    private fun addResult(message: String) {
        _testResults.value = _testResults.value + message
        Log.d("SyncCursorTest", message)
    }
}
```

---

## ✅ Test 3: Verify via Database Directly

### iOS SQL Queries
```sql
-- Check table structure
.schema sync_cursors

-- Count cursors
SELECT COUNT(*) FROM sync_cursors;

-- List all cursors
SELECT * FROM sync_cursors;

-- Check specific collection
SELECT * FROM sync_cursors WHERE collection = 'test_routines';
```

### Android SQL Queries (via Database Inspector)
```sql
-- Count cursors
SELECT COUNT(*) FROM sync_cursors;

-- List all cursors
SELECT * FROM sync_cursors;

-- Check specific collection
SELECT * FROM sync_cursors WHERE collection = 'test_routines';
```

---

## ✅ Test 4: Verify via Logs

### iOS
Check Xcode console for:
- `"Updated sync cursor for collection 'test_routines' to [date]"`
- `"Deleted sync cursor for collection 'test_routines'"`

### Android
Check Logcat for:
- `"Updated sync cursor for collection 'test_routines' to [date]"`
- `"Deleted sync cursor for collection 'test_routines'"`

Filter by tag: `Database`

---

## 📋 Test Checklist

- [ ] **Test 1**: Database is empty initially (0 cursors)
- [ ] **Test 2**: Can create a cursor
- [ ] **Test 3**: Can read a cursor
- [ ] **Test 4**: Can update a cursor (timestamp changes)
- [ ] **Test 5**: Can get all cursors
- [ ] **Test 6**: Can delete a cursor
- [ ] **Test 7**: Database is empty after cleanup (0 cursors)
- [ ] **Test 8**: Multiple cursors work (create 2-3 different collections)
- [ ] **Test 9**: Cursor persists after app restart
- [ ] **Test 10**: Database migration worked (v3 → v4)

---

## 🐛 Troubleshooting

### Issue: Table doesn't exist
**Solution:** Check that migration v4 ran. Look for log: `"Database schema v4 created - added sync_cursors table"`

### Issue: Can't read cursor after creating
**Solution:** Check that `collection` field matches exactly (case-sensitive)

### Issue: Update doesn't change timestamp
**Solution:** Verify you're using the same `collection` name and that the new date is actually different

---

## 🎯 Success Criteria

All tests should pass:
- ✅ Database starts empty
- ✅ Can create cursors
- ✅ Can read cursors
- ✅ Can update cursors
- ✅ Can delete cursors
- ✅ Database ends empty after cleanup
- ✅ Cursors persist across app restarts

---

## Next Steps

After verifying CRUD works locally:
1. ✅ Phase 3.1 complete
2. → Move to Phase 3.2: Upload Queue implementation
3. → Move to Phase 3.3: Pull Cursor implementation
