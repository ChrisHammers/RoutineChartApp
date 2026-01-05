# Features

This directory contains feature modules, each self-contained with its own:
- Domain layer (entities, use cases specific to feature)
- Data layer (repositories, data sources)
- Presentation layer (ViewModels, Views)

## Feature Modules

### Auth
- Parent sign up/sign in
- Child sign in
- Authentication state management

### Onboarding
- Family creation flow
- First-time setup

### Parent
- Dashboard (routine list, child switcher)
- RoutineBuilder (create/edit routines)
- Analytics (per child, per routine)
- QRInvite (generate QR code for family join)

### Child
- Join (scan QR, create child account)
- TodayView (list of assigned routines for today)
- RoutineRun (step-by-step completion interface)

## Structure

Each feature follows:
```
FeatureName/
├── Domain/
│   ├── Entities/ (feature-specific models)
│   └── UseCases/ (feature-specific logic)
├── Data/
│   ├── Repositories/
│   └── DataSources/
└── Presentation/
    ├── ViewModels/
    └── Views/
```

