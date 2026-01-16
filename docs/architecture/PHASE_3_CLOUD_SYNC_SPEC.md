# Phase 3: Cloud Sync - Specification

**Status:** ⏳ PENDING  
**Next Phase After:** Phase 2.3 (User Linking & Firestore Sync)

---

## Overview

Phase 3 implements cloud synchronization for all domain models (Routines, RoutineSteps, RoutineAssignments, CompletionEvents, ChildProfiles). This enables multi-device synchronization, offline → online sync, and ensures data consistency across all devices in a family.

---

## Objectives

1. **Upload Queue**: Push unsynced local changes to Firestore
2. **Pull Cursor**: Download remote changes since last sync
3. **Merge Logic**: Apply remote changes to local database
4. **Conflict Resolution**: Handle concurrent edits gracefully
5. **Background Sync**: Sync automatically when online

---

## Sync Architecture

### Upload Phase

Local events/changes are marked with a `synced` flag (local only, not in domain model):
- `synced = false`: Created locally, not yet uploaded
- `synced = true`: Successfully uploaded to cloud

**Upload Process:**
```
1. Query local DB for unsynced records (synced = false)
2. For each unsynced record:
   a. Upload to Firestore
   b. Mark as synced in local DB
3. Handle errors gracefully (retry on next sync)
```

### Pull Cursor

Each device tracks the last sync timestamp per collection:
```
SyncCursor {
  collection: String (routines | steps | assignments | events | children)
  lastSyncedAt: Timestamp
}
```

**Download Process:**
```
1. Get cursor for collection (lastSyncedAt)
2. Query Firestore for records where updatedAt > lastSyncedAt
3. For each remote record:
   a. Apply merge logic
   b. Insert/update in local DB
4. Update cursor to current timestamp
```

### Merge Rules

#### CompletionEvents (Append-Only)
- **No conflicts**: Append-only log, never modified
- Simply insert all new events into local database
- Derived state recomputed automatically
- Ordering: `(eventAt ASC, eventId ASC, deviceId ASC)`

#### Routines/Steps/Assignments (Last-Write-Wins)
- **Last-write-wins** based on `updatedAt` timestamp
- If `remote.updatedAt > local.updatedAt`: overwrite local
- Parent edits always sync from server (parents don't compete)
- Soft deletes: Set `deletedAt` timestamp, never hard-delete

#### ChildProfiles (Last-Write-Wins)
- Similar to Routines: last-write-wins based on `updatedAt`
- Parents can edit, children cannot

---

## Firestore Collections

### Subcollections (Family-Scoped)

#### `/families/{familyId}/routines/{routineId}`
```json
{
  "id": "routine123",
  "familyId": "family123",
  "title": "Morning Routine",
  "iconName": "sun",
  "version": 1,
  "completionRule": "all_steps_required",
  "createdAt": "2026-01-05T10:00:00Z",
  "updatedAt": "2026-01-05T10:00:00Z",
  "deletedAt": null
}
```

#### `/families/{familyId}/routines/{routineId}/steps/{stepId}`
```json
{
  "id": "step123",
  "routineId": "routine123",
  "orderIndex": 0,
  "label": "Brush Teeth",
  "iconName": "tooth",
  "audioCueUrl": null,
  "createdAt": "2026-01-05T10:00:00Z",
  "deletedAt": null
}
```

#### `/families/{familyId}/assignments/{assignmentId}`
```json
{
  "id": "assignment123",
  "familyId": "family123",
  "routineId": "routine123",
  "childId": "child123",
  "isActive": true,
  "assignedAt": "2026-01-05T10:00:00Z",
  "deletedAt": null
}
```

#### `/families/{familyId}/events/{eventId}`
```json
{
  "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
  "familyId": "family123",
  "childId": "child123",
  "routineId": "routine123",
  "stepId": "step123",
  "eventType": "complete",
  "eventAt": "2026-01-05T10:00:00Z",
  "localDayKey": "2026-01-05",
  "deviceId": "device123"
}
```

#### `/families/{familyId}/children/{childId}`
```json
{
  "id": "child123",
  "familyId": "family123",
  "displayName": "Emma",
  "avatarIcon": "star",
  "ageBand": "5_7",
  "readingMode": "light_text",
  "audioEnabled": true,
  "createdAt": "2026-01-05T10:00:00Z"
}
```

---

## Implementation Plan

### Phase 3.1: Sync Infrastructure
- [ ] Create `SyncCursor` model (collection, lastSyncedAt)
- [ ] Create `SyncService` protocol/interface
- [ ] Implement cursor storage (local DB)
- [ ] Implement sync state tracking

### Phase 3.2: Upload Queue (Routines)
- [ ] Add `synced` flag to local Routine records
- [ ] Implement `uploadUnsyncedRoutines()` method
- [ ] Mark routines as synced after upload
- [ ] Handle upload errors (retry logic)

### Phase 3.3: Pull Cursor (Routines)
- [ ] Implement `pullRoutines()` method
- [ ] Query Firestore for routines updated since cursor
- [ ] Apply merge logic (last-write-wins)
- [ ] Update cursor after successful pull

### Phase 3.4: Sync RoutineSteps
- [ ] Upload unsynced steps
- [ ] Pull remote steps
- [ ] Handle step deletions (soft delete)
- [ ] Maintain orderIndex consistency

### Phase 3.5: Sync RoutineAssignments
- [ ] Upload unsynced assignments
- [ ] Pull remote assignments
- [ ] Handle assignment deletions (soft delete)

### Phase 3.6: Sync CompletionEvents
- [ ] Upload unsynced events (append-only)
- [ ] Pull remote events since cursor
- [ ] Insert events in correct order
- [ ] Recompute derived state after sync

### Phase 3.7: Sync ChildProfiles
- [ ] Upload unsynced child profiles
- [ ] Pull remote child profiles
- [ ] Apply merge logic (last-write-wins)

### Phase 3.8: Background Sync
- [ ] Implement automatic sync on app launch
- [ ] Implement periodic sync (every N minutes)
- [ ] Implement sync on network connectivity change
- [ ] Show sync status in UI (optional)

### Phase 3.9: Conflict Resolution
- [ ] Handle concurrent edits gracefully
- [ ] Last-write-wins for routines/steps/assignments
- [ ] Append-only for events (no conflicts)
- [ ] Log conflicts for debugging

---

## Files to Create

### iOS
- `Core/Data/Remote/Firebase/FirestoreRoutineSyncService.swift`
- `Core/Data/Remote/Firebase/FirestoreRoutineStepSyncService.swift`
- `Core/Data/Remote/Firebase/FirestoreAssignmentSyncService.swift`
- `Core/Data/Remote/Firebase/FirestoreEventSyncService.swift`
- `Core/Data/Remote/Firebase/FirestoreChildProfileSyncService.swift`
- `Core/Data/Remote/Firebase/SyncCursorManager.swift`
- `Core/Data/Remote/Firebase/SyncService.swift`
- `Core/Data/Local/Database/SyncCursor.swift` (model)
- `Core/Data/Local/Database/DatabaseExtensions.swift` (add synced flag)

### Android
- `core/data/remote/firebase/FirestoreRoutineSyncService.kt`
- `core/data/remote/firebase/FirestoreRoutineStepSyncService.kt`
- `core/data/remote/firebase/FirestoreAssignmentSyncService.kt`
- `core/data/remote/firebase/FirestoreEventSyncService.kt`
- `core/data/remote/firebase/FirestoreChildProfileSyncService.kt`
- `core/data/remote/firebase/SyncCursorManager.kt`
- `core/data/remote/firebase/SyncService.kt`
- `core/data/local/room/entities/SyncCursorEntity.kt`
- `core/data/local/room/entities/` (add synced flag to existing entities)

---

## Firestore Security Rules

### Routines
```firestore
match /families/{familyId}/routines/{routineId} {
  allow read: if belongsToFamily(familyId);
  allow create, update, delete: if isParentInFamily(familyId);
}
```

### Steps
```firestore
match /families/{familyId}/routines/{routineId}/steps/{stepId} {
  allow read: if belongsToFamily(familyId);
  allow create, update, delete: if isParentInFamily(familyId);
}
```

### Assignments
```firestore
match /families/{familyId}/assignments/{assignmentId} {
  allow read: if belongsToFamily(familyId);
  allow create, update, delete: if isParentInFamily(familyId);
}
```

### Events
```firestore
match /families/{familyId}/events/{eventId} {
  allow read: if belongsToFamily(familyId);
  allow create: if belongsToFamily(familyId) 
                && request.resource.data.childId == request.auth.uid
                && request.resource.data.eventType in ['complete', 'undo'];
  allow update, delete: if false; // Events are append-only
}
```

### Children
```firestore
match /families/{familyId}/children/{childId} {
  allow read: if belongsToFamily(familyId);
  allow create, update, delete: if isParentInFamily(familyId);
}
```

---

## Firestore Indexes

Required indexes for efficient queries:

1. **Events by familyId and eventAt:**
   ```
   Collection: events
   Fields: familyId (ASC), eventAt (ASC)
   ```

2. **Events by familyId, childId, and eventAt:**
   ```
   Collection: events
   Fields: familyId (ASC), childId (ASC), eventAt (ASC)
   ```

3. **Events by familyId, localDayKey, and eventAt:**
   ```
   Collection: events
   Fields: familyId (ASC), localDayKey (ASC), eventAt (ASC)
   ```

4. **Routines by familyId, deletedAt, and updatedAt:**
   ```
   Collection: routines
   Fields: familyId (ASC), deletedAt (ASC), updatedAt (DESC)
   ```

5. **Assignments by familyId, childId, and isActive:**
   ```
   Collection: assignments
   Fields: familyId (ASC), childId (ASC), isActive (ASC)
   ```

---

## Testing Scenarios

### Scenario 1: Multi-Device Sync
1. Parent creates routine on Device A
2. Routine syncs to Firestore
3. Parent opens app on Device B
4. Routine appears on Device B (pulled from Firestore)

### Scenario 2: Offline → Online Sync
1. Child completes step while offline
2. Event saved locally (synced = false)
3. Device goes online
4. Event uploads to Firestore
5. Event appears on other devices

### Scenario 3: Concurrent Edits
1. Parent edits routine on Device A
2. Parent edits same routine on Device B
3. Both devices sync
4. Last-write-wins: Device B's changes overwrite Device A's

### Scenario 4: Event Ordering
1. Multiple events created on different devices
2. Events sync to Firestore
3. Events pulled in correct order (eventAt, eventId, deviceId)
4. Derived state is consistent

---

## Success Criteria

- [ ] All local changes upload to Firestore
- [ ] All remote changes download to local DB
- [ ] Merge logic works correctly (last-write-wins)
- [ ] Events maintain correct ordering
- [ ] Offline → online sync works
- [ ] Multi-device sync works
- [ ] Conflict resolution works
- [ ] Background sync works
- [ ] No data loss during sync
- [ ] Performance is acceptable (< 5s for full sync)

---

## Next Phase: Phase 4 - Analytics

After Phase 3, Phase 4 will implement:
- Days practiced calculation
- Routine completion counts
- Per-child/per-routine metrics
- Analytics UI

---

**Status:** Ready to begin Phase 3 implementation
