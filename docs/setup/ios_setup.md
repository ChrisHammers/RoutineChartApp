# iOS Setup Guide

## Prerequisites

- Xcode 15.0+
- iOS 17.0+ deployment target
- CocoaPods or Swift Package Manager
- Firebase project configured

---

## Initial Setup

### 1. Open Project

```bash
cd ios/RoutineChart
open RoutineChart.xcodeproj
```

### 2. Install Dependencies

The project uses Swift Package Manager for dependencies.

**Required Packages:**

1. **GRDB.swift** - SQLite toolkit
   - URL: `https://github.com/groue/GRDB.swift`
   - Version: 6.24.0 or later

2. **Firebase iOS SDK**
   - URL: `https://github.com/firebase/firebase-ios-sdk`
   - Products needed:
     - FirebaseAuth
     - FirebaseFirestore
     - FirebaseFunctions

3. **swift-ulid** - ULID generator
   - URL: `https://github.com/yaslab/ULID.swift`
   - Version: 1.3.0 or later

**To Add in Xcode:**
1. File → Add Package Dependencies
2. Enter package URL
3. Select version
4. Choose target: RoutineChart

### 3. Configure Firebase

1. Download `GoogleService-Info.plist` from Firebase Console
2. Add to `ios/RoutineChart/RoutineChart/` directory
3. Ensure it's added to RoutineChart target

### 4. Setup SwiftLint

**Install SwiftLint:**

```bash
brew install swiftlint
```

**Add Build Phase:**
1. In Xcode, select RoutineChart target
2. Build Phases → + → New Run Script Phase
3. Add script:

```bash
if which swiftlint >/dev/null; then
  swiftlint
else
  echo "warning: SwiftLint not installed, download from https://github.com/realm/SwiftLint"
fi
```

4. Move the phase before "Compile Sources"

**SwiftLint Configuration:**

The `.swiftlint.yml` file is in the project root with our coding standards.

---

## Project Structure

```
ios/RoutineChart/RoutineChart/
├── App/
│   ├── RoutineChartApp.swift (entry point)
│   └── AppDependencies.swift (DI container)
├── Core/
│   ├── Domain/
│   │   ├── Models/ (domain entities)
│   │   ├── Repositories/ (repository protocols)
│   │   └── UseCases/ (business logic)
│   ├── Data/
│   │   ├── Local/
│   │   │   ├── Database/ (SQLite/GRDB)
│   │   │   └── Repositories/ (local implementations)
│   │   └── Remote/
│   │       ├── Firebase/ (Firestore wrapper)
│   │       └── Repositories/ (remote implementations)
│   ├── Utils/
│   │   ├── ULID.swift
│   │   ├── DateHelpers.swift
│   │   └── Logger.swift
│   └── Sync/
│       ├── SyncEngine.swift
│       ├── UploadQueue.swift
│       └── PullCursor.swift
└── Features/
    ├── Auth/
    │   ├── Domain/
    │   ├── Data/
    │   └── Presentation/
    │       ├── ViewModels/
    │       └── Views/
    ├── Onboarding/
    ├── Parent/
    │   ├── Dashboard/
    │   ├── RoutineBuilder/
    │   ├── Analytics/
    │   └── QRInvite/
    └── Child/
        ├── Join/
        ├── TodayView/
        └── RoutineRun/
```

---

## Building & Running

### Development Build

1. Select RoutineChart scheme
2. Choose simulator or device
3. Cmd+R to build and run

### Testing

**Run Unit Tests:**
```bash
Cmd+U in Xcode
```

**Run UI Tests:**
```bash
Select RoutineChartUITests scheme
Cmd+U
```

---

## Code Style

### Naming Conventions

- **Views:** `{Feature}View.swift` (e.g., `ParentDashboardView.swift`)
- **ViewModels:** `{Feature}ViewModel.swift` (e.g., `ParentDashboardViewModel.swift`)
- **UseCases:** `{Action}UseCase.swift` (e.g., `CompleteStepUseCase.swift`)
- **Repositories:** `{Domain}Repository.swift` (e.g., `RoutineRepository.swift`)

### Architecture

- **MVVM + Clean Architecture**
- Views use `@StateObject` for ViewModels
- ViewModels use `@Published` for observable state
- Domain layer is independent of UI and frameworks
- Repositories provide data abstraction

### SwiftUI Guidelines

- Use `@StateObject` for ViewModel creation
- Use `@ObservedObject` when passing ViewModels down
- Keep views focused on UI logic only
- No direct API/database calls in views
- Always include PreviewProvider

---

## Database Migrations

Migrations are in `Core/Data/Local/Database/Migrations/`

**To add a migration:**

1. Create new migration file: `Migration_vX.swift`
2. Implement schema changes
3. Increment database version in `SQLiteManager.swift`
4. Test migration with existing data

---

## Logging

Use `Logger` utility for all logging:

```swift
import OSLog

let logger = Logger(subsystem: "com.routinechart.app", category: "sync")
logger.info("Syncing events...")
logger.error("Sync failed: \(error)")
```

---

## Troubleshooting

### Build Errors

**"GRDB not found"**
- Verify package is added in Project Settings → Package Dependencies
- Clean build folder: Shift+Cmd+K

**"Firebase not configured"**
- Ensure `GoogleService-Info.plist` is in project
- Check it's added to target membership

### Runtime Issues

**"Database locked"**
- Ensure only one database connection
- Check SQLiteManager is singleton

**"Firestore permission denied"**
- Check security rules in Firebase Console
- Verify user is authenticated

---

## Resources

- [SwiftUI Documentation](https://developer.apple.com/documentation/swiftui)
- [GRDB Documentation](https://github.com/groue/GRDB.swift)
- [Firebase iOS Guide](https://firebase.google.com/docs/ios/setup)
- [Architecture Spec](../architecture/Cloud_Data_Model_and_Sync_Spec_V1_Expanded.md)

---

**Last Updated:** 2026-01-03

