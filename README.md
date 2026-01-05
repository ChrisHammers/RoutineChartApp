# Routine Chart App

A visual-first routine tracking app for families, helping children complete daily routines independently. Built with offline-first architecture and cloud sync.

## Overview

Routine Chart helps parents create visual routines that children can follow and complete on their own devices. The app uses event-sourcing for reliable completion tracking and syncs across all family devices via Firebase.

### Key Features

- **Visual-first design** - Routines work without reading
- **Multi-device support** - Children can use their own devices
- **QR family joining** - Easy onboarding for kids
- **Offline-first** - Works without internet, syncs when online
- **Event-sourced completion** - Reliable, never-lose-data tracking
- **Light analytics** - Calm, non-competitive progress metrics

## Project Structure

```
RoutineChartApp/
├── ios/                    # iOS app (SwiftUI)
├── android/                # Android app (Jetpack Compose)
├── backend/                # Firebase (Firestore, Cloud Functions)
├── docs/                   # Documentation
│   ├── architecture/       # Architecture specs
│   ├── setup/              # Setup guides
│   └── testing/            # QA checklists
└── .github/                # CI/CD workflows
```

## Tech Stack

### iOS
- **UI:** SwiftUI
- **Architecture:** MVVM + Clean Architecture
- **Database:** SQLite (GRDB.swift)
- **Backend:** Firebase (Auth, Firestore, Functions)
- **Dependencies:** Swift Package Manager

### Android
- **UI:** Jetpack Compose + Material3
- **Architecture:** MVI + Clean Architecture
- **Database:** Room
- **Backend:** Firebase (Auth, Firestore, Functions)
- **Dependencies:** Gradle + Hilt

### Backend
- **Auth:** Firebase Authentication
- **Database:** Cloud Firestore
- **Functions:** Cloud Functions (TypeScript)
- **Storage:** Firebase Storage (for audio cues, future)

## Getting Started

### Prerequisites

- **iOS:** Xcode 15.0+, iOS 17.0+
- **Android:** Android Studio Hedgehog (2023.1.1)+, JDK 17+
- **Firebase:** Firebase project with Firestore and Auth enabled
- **Node.js:** 18+ (for Cloud Functions)

### Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/christopherhammers/RoutineChartApp.git
   cd RoutineChartApp
   ```

2. **Set up Firebase:**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable Authentication (Email/Password)
   - Enable Firestore
   - See [docs/setup/firebase_setup.md](docs/setup/firebase_setup.md) for details

3. **iOS Setup:**
   - Download `GoogleService-Info.plist` from Firebase Console
   - Place in `ios/RoutineChart/RoutineChart/`
   - Open `ios/RoutineChart/RoutineChart.xcodeproj` in Xcode
   - Xcode will resolve Swift Package Manager dependencies automatically
   - See [docs/setup/ios_setup.md](docs/setup/ios_setup.md) for details

4. **Android Setup:**
   - Download `google-services.json` from Firebase Console
   - Place in `android/app/`
   - Open `android/` in Android Studio
   - Gradle will sync dependencies automatically
   - See [docs/setup/android_setup.md](docs/setup/android_setup.md) for details

5. **Backend Setup:**
   ```bash
   cd backend/functions
   npm install
   cd ..
   firebase login
   firebase deploy
   ```
   - See [docs/setup/firebase_setup.md](docs/setup/firebase_setup.md) for details

## Running the Apps

### iOS
```bash
cd ios/RoutineChart
open RoutineChart.xcodeproj
# Press Cmd+R in Xcode to build and run
```

### Android
```bash
cd android
./gradlew installDebug
```

Or open in Android Studio and click Run.

## Development

### Project Phases

The project is being built in phases:

- **Phase 0 (Current):** Repository foundation, project setup ✅
- **Phase 1:** Domain models, local persistence, event-sourced completion
- **Phase 2:** Authentication, QR family joining
- **Phase 3:** Cloud backend, sync engine
- **Phase 4:** Light analytics
- **Phase 5:** Pricing gate (free: 3 routines, paid: unlimited)

### Architecture

See [docs/architecture/Cloud_Data_Model_and_Sync_Spec_V1_Expanded.md](docs/architecture/Cloud_Data_Model_and_Sync_Spec_V1_Expanded.md) for the canonical architecture specification.

**Key Principles:**
- Offline-first: All operations work without network
- Event-sourcing: Completion state derived from append-only event log
- Family-scoped: All data belongs to a family namespace
- Role-based access: Parents edit routines, children complete steps
- ULID-based ordering: Deterministic event ordering across devices

### Code Style

**iOS:**
- SwiftLint enforced on build
- MVVM + Clean Architecture
- View → ViewModel → UseCase → Repository → DataSource

**Android:**
- ktlint enforced via Gradle
- MVI + Clean Architecture
- Screen → ViewModel (State/Event/Intent) → UseCase → Repository → DataSource

### Testing

**iOS:**
```bash
xcodebuild test -scheme RoutineChart -destination 'platform=iOS Simulator,name=iPhone 15'
```

**Android:**
```bash
cd android
./gradlew test
./gradlew connectedAndroidTest
```

## Contributing

1. Create a feature branch from `develop`
2. Follow the code style guidelines (SwiftLint/ktlint)
3. Write tests for new functionality
4. Submit a pull request

See [.github/pull_request_template.md](.github/pull_request_template.md) for PR requirements.

## Documentation

- [Architecture Spec](docs/architecture/Cloud_Data_Model_and_Sync_Spec_V1_Expanded.md)
- [Shared Enums](docs/shared_enums.md)
- [iOS Setup Guide](docs/setup/ios_setup.md)
- [Android Setup Guide](docs/setup/android_setup.md)
- [Firebase Setup Guide](docs/setup/firebase_setup.md)
- [QA Checklist](docs/testing/qa_checklist.md)

## License

Proprietary - All rights reserved.

## Contact

For questions or issues, contact: [@christopherhammers](https://github.com/christopherhammers)

---

**Status:** Phase 0 & Phase 1 (iOS + Android) Complete ✅  
**Next Phase:** Phase 2 - Authentication + QR Join

## 🎉 Current Progress

- ✅ **Phase 0 Complete** - Repository foundation, build configs, documentation
- ✅ **Phase 1 (iOS) Complete** - Domain models, SQLite, event-sourcing, working UI
- ✅ **Phase 1 (Android) Complete** - Domain models, Room, event-sourcing, working UI
- ⏳ **Phase 2 Pending** - Firebase Auth, QR family join
- ⏳ **Phase 3 Pending** - Cloud sync engine
- ⏳ **Phase 4 Pending** - Light analytics
- ⏳ **Phase 5 Pending** - Pricing enforcement

**Overall Progress:** 40% (Phase 0 + Phase 1 both platforms)

**See:** [`docs/IMPLEMENTATION_COMPLETE.md`](docs/IMPLEMENTATION_COMPLETE.md) for full details
