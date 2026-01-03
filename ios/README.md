# Routine Chart - iOS App

iOS app for Routine Chart built with SwiftUI, GRDB (SQLite), and Firebase.

## Setup

1. **Requirements:**
   - Xcode 15.0+
   - iOS 17.0+ deployment target
   - CocoaPods or Swift Package Manager

2. **Open Project:**
   ```bash
   cd ios/RoutineChart
   open RoutineChart.xcodeproj
   ```

3. **Install Dependencies:**
   - Dependencies are managed via Swift Package Manager
   - Xcode will automatically resolve packages on first build

4. **Firebase Configuration:**
   - Download `GoogleService-Info.plist` from Firebase Console
   - Add to `ios/RoutineChart/RoutineChart/` directory

5. **Install SwiftLint:**
   ```bash
   brew install swiftlint
   ```

## Project Structure

- `App/` - Application entry point and DI
- `Core/` - Shared domain models, data layer, sync
  - `Domain/` - Business logic and models
  - `Data/` - Local and remote data sources
  - `Utils/` - Helper utilities
  - `Sync/` - Sync engine
- `Features/` - Feature modules
  - `Auth/` - Authentication
  - `Parent/` - Parent dashboard, routine builder
  - `Child/` - Child today view, routine run

## Architecture

- **MVVM + Clean Architecture**
- **SwiftUI** - Declarative UI framework
- **GRDB** - SQLite toolkit for local persistence
- **Firebase** - Backend and authentication
- **Event-Sourcing** - Completion state derived from event log

## Testing

Run tests in Xcode: `Cmd+U`

Or via command line:
```bash
xcodebuild test -scheme RoutineChart -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Linting

SwiftLint runs automatically on build.

Manual check:
```bash
swiftlint lint
```

Auto-fix:
```bash
swiftlint --fix
```

## Documentation

See [docs/setup/ios_setup.md](../docs/setup/ios_setup.md) for detailed setup instructions.

