# Phase 3.5 iOS Testing Checklist

**Status:** ✅ READY FOR TESTING  
**Phase:** 3.5 – Sync RoutineAssignments (Upload & Pull)

---

## Overview

Phase 3.5 adds Firestore sync for **RoutineAssignment** entities:

- **Upload:** Unsynced assignments (create/update/soft-delete) are uploaded to Firestore.
- **Pull:** Assignments updated in Firestore since the last cursor are pulled and merged (last-write-wins by `updatedAt`).
- **Collection:** Top-level `routine_assignments` (document ID = assignment ID). Each document has `familyId`, `routineId`, `childId`, `isActive`, `assignedAt`, `updatedAt`, `deletedAt`.

Sync runs on app launch for the current user (after routine sync), for both existing users and new users.

---

## Phase 3.5: Sync RoutineAssignments – Testing

### Test 1: Upload New Assignment

**Objective:** Verify creating an assignment locally uploads it to Firestore.

**Prerequisites:**

- Signed in as Parent with a family, at least one routine, and at least one child profile.

**Steps:**

1. In the app, assign a routine to a child (e.g. from Parent Dashboard or routine builder).
2. Check logs for: `Created assignment locally (will sync via upload queue)` and `🔄 Starting upload of unsynced assignments`.
3. Open Firestore Console → `routine_assignments` collection.
4. Find a document with the same ID as the new assignment; confirm `familyId`, `routineId`, `childId`, `assignedAt`, `updatedAt` match.

**Expected Results:**

- ✅ Log: `📤 Found X unsynced assignment(s) to upload` (X ≥ 1).
- ✅ Log: `✅ Uploaded assignment: [assignmentId]`.
- ✅ Document exists in `routine_assignments` with correct fields.
- ✅ Local assignment is marked synced (no duplicate upload on next launch).

---

### Test 2: Pull Assignments (Initial – No Cursor)

**Objective:** Verify assignments in Firestore are pulled when there is no cursor (or first sync).

**Steps:**

1. In Firestore Console, add a document to `routine_assignments` with valid `familyId`, `routineId`, `childId`, `isActive`, `assignedAt`, `updatedAt` (and optional `deletedAt`). Use a `familyId` that matches the device’s current user.
2. Force quit the app, clear app data or use a device/profile that has never synced assignments for this family, then launch the app.
3. Check logs for: `🔄 Starting pull of assignments from Firestore`.
4. In the app, confirm the assignment appears (e.g. child’s assigned routines or parent’s assignment list).

**Expected Results:**

- ✅ Log: `📥 Found X assignment(s) to pull from Firestore` (X ≥ 1).
- ✅ Log: `✅ Inserted new assignment from Firestore: [assignmentId]` or `✅ Merged assignment (remote wins)`.
- ✅ Log: `✅ Pull complete: merged X assignment(s)`.
- ✅ Assignment is visible in the UI where expected.

---

### Test 3: Incremental Pull (With Cursor)

**Objective:** Verify only assignments updated after the last sync are pulled.

**Steps:**

1. Ensure the app has already synced assignments at least once (cursor exists).
2. In Firestore, add or update an assignment and set `updatedAt` to a recent timestamp.
3. Launch the app (or trigger load so assignment sync runs).
4. Check logs for cursor usage and pull count.

**Expected Results:**

- ✅ Only assignments with `updatedAt` > last cursor are returned.
- ✅ Log: `📥 Found X assignment(s) to pull` with X matching new/updated documents only.
- ✅ No duplicate assignment rows; existing assignments are updated, not re-inserted.

---

### Test 4: Merge Logic (Remote Wins)

**Objective:** When remote has a newer `updatedAt`, local assignment is overwritten with Firestore data.

**Steps:**

1. Create an assignment locally and wait for it to upload.
2. In Firestore, edit that assignment (e.g. set `isActive` to `false` or change `updatedAt` to a later time).
3. In the app, optionally change the same assignment locally (e.g. toggle active) so local `updatedAt` is older than Firestore.
4. Launch the app to trigger pull.
5. Check the assignment in the UI and in logs.

**Expected Results:**

- ✅ Local assignment matches Firestore (e.g. `isActive` and other fields).
- ✅ Log: `✅ Merged assignment (remote wins): [assignmentId]`.
- ✅ No duplicate; single row updated.

---

### Test 5: Local Wins (Skip Remote)

**Objective:** When local has a newer `updatedAt`, remote change is skipped and local is preserved.

**Steps:**

1. Create an assignment locally and wait for upload.
2. In the app, update the assignment (e.g. toggle `isActive`). Ensure this happens so local `updatedAt` is newer than Firestore.
3. In Firestore, change the same assignment (e.g. different `isActive`) so Firestore has an older `updatedAt`.
4. Launch the app to trigger pull.
5. Check the assignment in the UI.

**Expected Results:**

- ✅ Local value is preserved (e.g. `isActive` stays as set on device).
- ✅ Log: `⏭️ Skipped assignment (local wins): [assignmentId]`.
- ✅ On next sync, local change is uploaded and Firestore is updated.

---

### Test 6: Soft Delete – Upload

**Objective:** Verify soft-deleting an assignment locally uploads `deletedAt` to Firestore.

**Steps:**

1. Create an assignment in the app and wait for it to upload.
2. In the app, remove the assignment (soft delete) so that `deletedAt` is set locally.
3. Check logs for upload of unsynced assignments.
4. In Firestore, open the same assignment document.

**Expected Results:**

- ✅ Assignment is in the upload queue (unsynced after soft delete).
- ✅ Log: `✅ Uploaded assignment: [assignmentId]` (or equivalent for soft-deleted row).
- ✅ Document in Firestore has `deletedAt` set to a timestamp.
- ✅ Locally, assignment is soft-deleted (not shown in active lists, or filtered by `deletedAt`).

---

### Test 7: Soft Delete – Pull

**Objective:** Verify an assignment soft-deleted in Firestore is applied locally when pulled.

**Steps:**

1. Have an assignment that exists locally and in Firestore.
2. In Firestore, set `deletedAt` to a timestamp and ensure `updatedAt` is newer than the device’s last assignment sync cursor.
3. Launch the app to trigger pull.
4. Check the app: the assignment should be treated as deleted (e.g. not listed as active for the child).

**Expected Results:**

- ✅ Log: assignment pulled/merged (e.g. `✅ Merged assignment (remote wins)` or inserted).
- ✅ Local row has `deletedAt` set.
- ✅ UI no longer shows this assignment as active where appropriate.

---

### Test 8: Sync on App Launch (Existing User)

**Objective:** Verify assignment sync runs on launch for an existing Parent user.

**Steps:**

1. Signed in as Parent with a family.
2. Force quit the app and relaunch.
3. Check logs right after user load and routine sync.

**Expected Results:**

- ✅ After routine upload/pull and seed (if any), log: `🔄 Starting upload of unsynced assignments` and/or `🔄 Starting pull of assignments from Firestore`.
- ✅ If there are unsynced assignments: `✅ Uploaded X assignment(s)` or similar.
- ✅ If there are remote updates: `✅ Pulled X assignment(s) from Firestore on app launch` (when X > 0).

---

### Test 9: Sync on New User Creation

**Objective:** Verify assignment sync runs after creating a new Parent user and family.

**Steps:**

1. Sign out. Sign in with a new account (or create one) so a new User and Family are created.
2. Check logs after “Created family and user for parent” and routine sync.

**Expected Results:**

- ✅ After routine sync and seed, assignment sync runs: `uploadUnsynced` and `pullAssignments` for the new `familyId`.
- ✅ No crash; zero assignments is valid (empty pull/upload).

---

## Firestore Collection

- **Collection:** `routine_assignments`
- **Document ID:** assignment `id`
- **Fields:** `id`, `familyId`, `routineId`, `childId`, `isActive`, `assignedAt`, `updatedAt`, `deletedAt` (optional).

Ensure security rules allow read/write for the signed-in user for this collection (e.g. by `familyId` or equivalent).

---

## Success Criteria

**Phase 3.5 complete when:**

- ✅ New assignments upload to Firestore.
- ✅ Unsynced updates and soft-deletes upload (including `deletedAt`).
- ✅ Assignments pull from Firestore (initial and incremental).
- ✅ Merge is last-write-wins by `updatedAt` (remote wins / local wins as above).
- ✅ Soft-deleted assignments sync in both directions.
- ✅ Sync runs on app launch for existing and new Parent users.
- ✅ No duplicate assignments; cursor and merge behave as expected.

---

## Next Steps After Testing

Once Phase 3.5 is confirmed complete:

- [ ] Document any issues in this file or in the issue tracker.
- [ ] Mark Phase 3.5 complete in `docs/architecture/PHASE_3_CLOUD_SYNC_SPEC.md`.
- [ ] Move to Phase 3.6 (Sync CompletionEvents) or another planned phase.
