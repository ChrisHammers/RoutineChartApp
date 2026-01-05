# iOS Troubleshooting Guide

## Build Error: Missing GRDB Target

**Error:** `Unable to resolve build file: BuildFile<PACKAGE-TARGET:GRDB...> (The workspace has a reference to a missing target with GUID 'PACKAGE-TARGET:GRDBSQLite')`

### Cause
The Xcode project has package dependencies configured, but they're not properly resolved.

### Solution: Remove Packages (Phase 0 Only)

**For Phase 0**, we don't need any external packages yet! The basic SwiftUI template should build without them.

#### Steps to Remove Packages:

1. **Open Xcode:**
   ```bash
   cd /Users/christopherhammers/Documents/GitHub/RoutineChartApp/ios/RoutineChart
   open RoutineChart.xcodeproj
   ```

2. **Remove Package Products from Target:**
   - Click **RoutineChart** project (blue icon at top of navigator)
   - Select **RoutineChart** target
   - Go to **Frameworks, Libraries, and Embedded Content** section
   - Remove ALL entries by clicking "-" button

3. **Remove Package Dependencies:**
   - Click **RoutineChart** project (blue icon)
   - Go to **Package Dependencies** tab
   - Select each package:
     - firebase-ios-sdk
     - GRDB
     - ULID
   - Click **"-"** button to remove each one

4. **Clean and Rebuild:**
   - Product → Clean Build Folder (Shift+Cmd+K)
   - File → Packages → Reset Package Caches
   - Product → Build (Cmd+B)

The app should now build successfully! ✅

---

## When to Add Packages Back

**Add packages at the START of Phase 1** when we begin implementing:
- Domain models (need ULID for event IDs)
- Local database (need GRDB for SQLite)
- Firebase integration (need Firebase SDK)

---

## Alternative: Create Truly Clean Project

If removing packages doesn't work, you can create a fresh Xcode project:

1. **File → New → Project**
2. Choose **iOS → App**
3. Name: `RoutineChart`
4. Interface: **SwiftUI**
5. Language: **Swift**
6. Save to: `ios/RoutineChart/` (replace existing)

Then manually add back:
- SwiftLint build phase
- The placeholder folders (Core/, Features/, etc.)

---

## xcode-select Error

**Error:** `xcode-select: error: tool 'xcodebuild' requires Xcode`

### Solution:
```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
```

This points command-line tools to your Xcode installation.

---

## Phase 0 Goal

For Phase 0, we just need:
- ✅ Project opens in Xcode
- ✅ Basic SwiftUI template builds
- ✅ App runs on simulator showing "Hello, world!"

**No external packages needed yet!**

We'll add dependencies in Phase 1 when we actually need them for database and domain logic.

