# Android Setup Guide

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 34 (minSdk 24, targetSdk 34)
- Gradle 8.2+
- Firebase project configured

---

## Initial Setup

### 1. Open Project

```bash
cd android
# Open in Android Studio or use command line
./gradlew build
```

### 2. Project Structure

```
android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/com/routinechart/
│       │   │   ├── app/
│       │   │   │   ├── RoutineChartApplication.kt
│       │   │   │   └── di/ (Hilt modules)
│       │   │   ├── core/
│       │   │   │   ├── domain/
│       │   │   │   │   ├── models/
│       │   │   │   │   ├── repositories/
│       │   │   │   │   └── usecases/
│       │   │   │   ├── data/
│       │   │   │   │   ├── local/
│       │   │   │   │   │   ├── room/
│       │   │   │   │   │   └── repositories/
│       │   │   │   │   └── remote/
│       │   │   │   │       ├── firebase/
│       │   │   │   │       └── repositories/
│       │   │   │   ├── sync/
│       │   │   │   └── utils/
│       │   │   └── features/
│       │   │       ├── auth/
│       │   │       ├── onboarding/
│       │   │       ├── parent/
│       │   │       └── child/
│       │   ├── res/
│       │   └── AndroidManifest.xml
│       ├── test/ (unit tests)
│       └── androidTest/ (instrumentation tests)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
```

---

## Dependencies

### Core Dependencies (build.gradle.kts)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

android {
    namespace = "com.routinechart"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.routinechart"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ULID
    implementation("com.github.azam:ulidj:1.0.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // QR Code
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.zxing:core:3.5.2")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

### Project-level build.gradle.kts

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

---

## Firebase Configuration

1. Download `google-services.json` from Firebase Console
2. Place in `android/app/` directory
3. Ensure `google-services` plugin is applied in `app/build.gradle.kts`

---

## Code Style & Linting

### ktlint Setup

**Add to project-level build.gradle.kts:**

```kotlin
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.0.1")
        android.set(true)
        outputColorName.set("RED")
    }
}
```

**Run linting:**

```bash
./gradlew ktlintCheck
./gradlew ktlintFormat  # Auto-fix issues
```

### detekt Setup (Static Analysis)

**Add to app/build.gradle.kts:**

```kotlin
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

detekt {
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
```

---

## Architecture

### MVI Pattern

**State, Event, Intent:**

```kotlin
// State (what the UI shows)
data class RoutineListState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Event (one-time actions)
sealed class RoutineListEvent {
    data class ShowError(val message: String) : RoutineListEvent()
    object NavigateToBuilder : RoutineListEvent()
}

// Intent (user actions)
sealed class RoutineListIntent {
    object LoadRoutines : RoutineListIntent()
    data class DeleteRoutine(val id: String) : RoutineListIntent()
    object CreateNewRoutine : RoutineListIntent()
}

// ViewModel
@HiltViewModel
class RoutineListViewModel @Inject constructor(
    private val getRoutinesUseCase: GetRoutinesUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(RoutineListState())
    val state: StateFlow<RoutineListState> = _state.asStateFlow()
    
    private val _events = Channel<RoutineListEvent>()
    val events = _events.receiveAsFlow()
    
    fun handleIntent(intent: RoutineListIntent) {
        when (intent) {
            is RoutineListIntent.LoadRoutines -> loadRoutines()
            // ...
        }
    }
}
```

### Hilt Dependency Injection

**Application class:**

```kotlin
@HiltAndroidApp
class RoutineChartApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase, etc.
    }
}
```

**Module example:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "routine_chart.db"
        ).build()
    }
}
```

---

## Building & Running

### Debug Build

```bash
./gradlew assembleDebug
./gradlew installDebug
```

### Run Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

### Generate APK

```bash
./gradlew assembleRelease
```

---

## Database Migrations

**Define migrations in Room:**

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE routines ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
    }
}

Room.databaseBuilder(context, AppDatabase::class.java, "routine_chart.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

---

## Naming Conventions

- **Screens:** `{Feature}Screen.kt` (e.g., `ParentDashboardScreen.kt`)
- **ViewModels:** `{Feature}ViewModel.kt`
- **UseCases:** `{Action}UseCase.kt`
- **Repositories:** `{Domain}Repository.kt` (interface) + `{Domain}RepositoryImpl.kt`

---

## Logging

Use `timber` for structured logging:

```kotlin
dependencies {
    implementation("com.jakewharton.timber:timber:5.0.1")
}

// In Application.onCreate()
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}

// Usage
Timber.d("Loading routines for family: $familyId")
Timber.e(exception, "Failed to sync events")
```

---

## Troubleshooting

### Build Errors

**"Duplicate class" errors:**
- Check for conflicting dependencies
- Run `./gradlew app:dependencies` to debug

**KSP errors:**
- Ensure KSP version matches Kotlin version
- Clean build: `./gradlew clean`

### Runtime Issues

**"Firestore permission denied":**
- Check security rules in Firebase Console
- Verify authentication token is valid

**Room database errors:**
- Check migration scripts
- Use `fallbackToDestructiveMigration()` during development only

---

## Resources

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Firebase Android Guide](https://firebase.google.com/docs/android/setup)
- [Hilt Documentation](https://developer.android.com/training/dependency-injection/hilt-android)
- [Architecture Spec](../architecture/Cloud_Data_Model_and_Sync_Spec_V1_Expanded.md)

---

**Last Updated:** 2026-01-03

