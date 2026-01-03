# Feature Modules

Feature-specific modules following MVI architecture.

## Structure

Each feature contains:
- `domain/` - Feature-specific models and use cases
- `data/` - Repositories and data sources
- `presentation/` - ViewModels, State, Events, Screens

## Features

### auth
- Parent sign up/in screens
- Child sign in screen
- Auth state management

### onboarding
- Family creation
- Initial setup flow

### parent
- `dashboard/` - Routine list, child switcher
- `routinebuilder/` - Create/edit routines
- `analytics/` - Stats and metrics
- `qrinvite/` - Generate QR codes

### child
- `join/` - QR scan and join flow
- `todayview/` - Today's routines
- `routinerun/` - Step completion interface

## MVI Pattern

Each screen follows:

```kotlin
// State - what UI shows
data class ScreenState(...)

// Event - one-time effects
sealed class ScreenEvent { ... }

// Intent - user actions
sealed class ScreenIntent { ... }

// ViewModel
class ScreenViewModel : ViewModel() {
    val state: StateFlow<ScreenState>
    val events: Flow<ScreenEvent>
    fun handleIntent(intent: ScreenIntent)
}
```

