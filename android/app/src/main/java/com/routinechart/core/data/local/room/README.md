# Room Database

Local SQLite database using Room persistence library.

## Files to be added:

- `AppDatabase.kt` - Main database class
- `entities/` - Entity classes (annotated with @Entity)
- `daos/` - Data Access Objects (interfaces with @Dao)
- `converters/` - Type converters for custom types

## Entities

Each entity maps to a database table:
- `FamilyEntity`
- `UserEntity`
- `ChildProfileEntity`
- `RoutineEntity`
- `RoutineStepEntity`
- `RoutineAssignmentEntity`
- `CompletionEventEntity`
- `SyncCursorEntity`

## DAOs

- `FamilyDao`
- `RoutineDao`
- `RoutineStepDao`
- `CompletionEventDao`
- `SyncCursorDao`

## Migrations

Define migrations in `AppDatabase.kt` when schema changes:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Schema changes
    }
}
```

