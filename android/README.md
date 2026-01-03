# Routine Chart - Android App

Android app for Routine Chart built with Jetpack Compose, Room, and Firebase.

## Setup

1. **Requirements:**
   - Android Studio Hedgehog (2023.1.1) or later
   - JDK 17+
   - Android SDK 34

2. **Install Dependencies:**
   ```bash
   cd android
   ./gradlew build
   ```

3. **Firebase Configuration:**
   - Download `google-services.json` from Firebase Console
   - Place in `android/app/` directory

4. **Run:**
   ```bash
   ./gradlew installDebug
   ```

## Project Structure

- `app/` - Main application module
  - `src/main/java/com/routinechart/`
    - `app/` - Application class and MainActivity
    - `core/` - Domain models, data layer, sync engine
    - `features/` - Feature modules (auth, parent, child)

## Architecture

- **MVI Pattern** - Model-View-Intent for state management
- **Clean Architecture** - Domain, Data, Presentation layers
- **Hilt** - Dependency injection
- **Room** - Local database
- **Compose** - Modern UI toolkit

## Testing

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew connectedAndroidTest
```

## Linting

```bash
# Check code style
./gradlew ktlintCheck

# Auto-fix issues
./gradlew ktlintFormat
```

## Documentation

See [docs/setup/android_setup.md](../docs/setup/android_setup.md) for detailed setup instructions.

