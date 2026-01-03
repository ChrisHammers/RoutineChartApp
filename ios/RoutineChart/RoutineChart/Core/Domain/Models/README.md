# Domain Models

This directory contains the core domain models (entities) used throughout the app.

## Files to be added in Phase 1:

- `Family.swift` - Family entity with timezone, planTier
- `User.swift` - User entity with role (parent/child)
- `ChildProfile.swift` - Child profile with ageBand, readingMode
- `Routine.swift` - Routine entity with title, version, completionRule
- `RoutineStep.swift` - Individual step within a routine
- `RoutineAssignment.swift` - Links routine to child
- `CompletionEvent.swift` - Event-sourced completion log entry

## Naming Convention

All domain models should:
- Be structs (value types)
- Conform to `Identifiable` and `Codable`
- Use clear, descriptive property names
- Include timestamps (createdAt, updatedAt, etc.)

