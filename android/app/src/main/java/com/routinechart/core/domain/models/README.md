# Domain Models

Core domain models (entities) for the Routine Chart app.

## Files to be added in Phase 1:

- `Family.kt` - Family entity
- `User.kt` - User entity with Role enum
- `ChildProfile.kt` - Child profile with AgeBand, ReadingMode enums
- `Routine.kt` - Routine entity with CompletionRule enum
- `RoutineStep.kt` - Step entity
- `RoutineAssignment.kt` - Assignment entity
- `CompletionEvent.kt` - Event entity with EventType enum

## Conventions

- Use data classes for immutability
- All enums should serialize to lowercase strings for Firestore compatibility
- Include timestamp fields (createdAt, updatedAt)
- Use nullable types appropriately (e.g., `email: String?` for child users)

