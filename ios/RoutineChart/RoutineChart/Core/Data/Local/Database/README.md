# Local Database

This directory contains SQLite database implementation using GRDB.swift.

## Files to be added:

- `SQLiteManager.swift` - Database connection and configuration
- `Migrations/` - Database migration scripts
- Schema definitions for all tables

## Tables

- `families` - Family information
- `users` - User accounts
- `child_profiles` - Child profiles
- `routines` - Routines
- `routine_steps` - Steps within routines
- `routine_assignments` - Routine-to-child assignments
- `completion_events` - Event log (append-only)
- `sync_cursors` - Sync state tracking

## Migrations

- Each migration in `Migrations/` folder
- Named `Migration_vX.swift` where X is version number
- Applied automatically on database open

