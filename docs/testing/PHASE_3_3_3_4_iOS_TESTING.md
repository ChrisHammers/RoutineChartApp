# Phase 3.3 & 3.4 iOS Testing Checklist

**Status:** ✅ READY FOR TESTING  
**Phases:** 3.3 (Pull Cursor - Routines) & 3.4 (Sync RoutineSteps - Upload & Pull)

---

## Phase 3.3: Pull Cursor (Routines) - Testing

### Test 1: Initial Pull (No Cursor)
**Objective:** Verify routines are pulled from Firestore when no cursor exists

**Steps:**
1. Clear app data or use a fresh install
2. Ensure you have routines in Firestore (created from another device or manually)
3. Launch the app
4. Check logs for: `🔄 Starting pull of routines from Firestore`
5. Verify: Routines appear in the app

**Expected Results:**
- ✅ Routines from Firestore appear in the app
- ✅ Log shows: `📥 Found X routine(s) to pull from Firestore`
- ✅ Log shows: `✅ Pull complete: merged X routine(s)`
- ✅ Sync cursor is created/updated

---

### Test 2: Incremental Pull (With Cursor)
**Objective:** Verify only new/updated routines are pulled

**Steps:**
1. Have routines already synced locally
2. Create or update a routine on another device (or in Firestore Console)
3. Wait a few seconds
4. Launch the app (or trigger pull manually)
5. Check logs for cursor timestamp

**Expected Results:**
- ✅ Only new/updated routines are pulled
- ✅ Log shows: `📥 Last sync timestamp: [previous timestamp]`
- ✅ Log shows: `📥 Found X routine(s) to pull` (only new ones)
- ✅ Existing routines are not duplicated

---

### Test 3: Merge Logic (Last-Write-Wins)
**Objective:** Verify remote changes overwrite local when remote is newer

**Steps:**
1. Create a routine locally (note the title)
2. Wait for it to upload to Firestore
3. Update the routine title in Firestore Console (or another device)
4. Wait a few seconds
5. Launch the app to trigger pull
6. Check the routine title

**Expected Results:**
- ✅ Local routine title is updated to match Firestore
- ✅ Log shows: `✅ Merged routine (remote wins): [routineId]`
- ✅ Log shows timestamp comparison: `remote: [newer], local: [older]`

---

### Test 4: Local Wins (Skip Remote)
**Objective:** Verify local changes are preserved when local is newer

**Steps:**
1. Create a routine locally
2. Update it locally (change title)
3. Before it uploads, update the same routine in Firestore (different title)
4. Launch the app to trigger pull
5. Check the routine title

**Expected Results:**
- ✅ Local routine title is preserved (not overwritten)
- ✅ Log shows: `⏭️ Skipped routine (local wins): [routineId]`
- ✅ Log shows timestamp comparison: `remote: [older], local: [newer]`
- ✅ Local changes will upload via upload queue

---

### Test 5: Pull on App Launch
**Objective:** Verify pull happens automatically on app launch

**Steps:**
1. Create a routine on another device (or in Firestore Console)
2. Close the app completely
3. Launch the app
4. Check logs

**Expected Results:**
- ✅ Pull is triggered automatically
- ✅ Log shows: `✅ Pulled X routine(s) from Firestore on app launch`
- ✅ New routines appear without manual refresh

---

## Phase 3.4: Sync RoutineSteps - Testing

### Test 6: Steps Upload with Routine
**Objective:** Verify steps are uploaded when routine is uploaded

**Steps:**
1. Create a new routine with 3-4 steps
2. Save the routine
3. Check logs for step upload
4. Verify in Firestore Console: `/routines/{routineId}/steps/{stepId}`

**Expected Results:**
- ✅ Routine uploads successfully
- ✅ Log shows: `📤 Found X unsynced step(s) to upload for routine: [routineId]`
- ✅ Log shows: `✅ Uploaded step: [stepId]` for each step
- ✅ Steps appear in Firestore under the routine's subcollection
- ✅ All steps are marked as synced

---

### Test 7: Steps Upload on App Launch
**Objective:** Verify unsynced steps are uploaded on app launch

**Steps:**
1. Create a routine with steps while offline (or before upload)
2. Close the app
3. Launch the app (while online)
4. Check logs

**Expected Results:**
- ✅ Routine uploads
- ✅ Steps upload after routine uploads
- ✅ Log shows: `✅ Uploaded X step(s) for routine: [routineId]`
- ✅ Steps appear in Firestore

---

### Test 8: Edit Routine - Update Steps
**Objective:** Verify step updates are synced

**Steps:**
1. Edit an existing routine
2. Modify an existing step (change label or icon)
3. Add a new step
4. Remove a step
5. Save the routine
6. Check logs and Firestore

**Expected Results:**
- ✅ Modified steps are updated (not duplicated)
- ✅ New steps are created
- ✅ Removed steps are soft-deleted (deletedAt set)
- ✅ Log shows step uploads
- ✅ Firestore reflects all changes

---

### Test 9: Steps Security Rules
**Objective:** Verify Firestore security rules allow step operations

**Steps:**
1. Create a routine with steps
2. Verify steps upload successfully
3. Check Firestore Console for steps under `/routines/{routineId}/steps/`

**Expected Results:**
- ✅ No "Missing or insufficient permissions" errors
- ✅ Steps are created in Firestore
- ✅ Steps are readable by authenticated users
- ✅ Only routine owner/parent can modify steps

---

### Test 10: Multi-Device Step Sync
**Objective:** Verify steps sync across devices

**Steps:**
1. Device A: Create a routine with steps
2. Wait for upload to complete
3. Device B: Launch app (or trigger pull)
4. Verify steps appear on Device B

**Expected Results:**
- ✅ Steps appear on Device B
- ✅ Step order is preserved (orderIndex)
- ✅ Step labels and icons are correct
- ✅ All steps are present

---

## Combined Tests

### Test 11: Full Sync Flow (Upload + Pull)
**Objective:** Verify complete bidirectional sync

**Steps:**
1. Device A: Create routine with steps
2. Wait for upload
3. Device B: Launch app
4. Device B: Edit routine (modify steps)
5. Wait for upload
6. Device A: Launch app (or trigger pull)
7. Verify changes sync

**Expected Results:**
- ✅ Device B receives routine and steps from Device A
- ✅ Device A receives updated routine and steps from Device B
- ✅ No duplicates
- ✅ All changes are preserved

---

### Test 12: Offline → Online Sync
**Objective:** Verify steps sync when going online

**Steps:**
1. Go offline (airplane mode)
2. Create a routine with steps
3. Edit the routine (add/modify steps)
4. Go online
5. Launch app or wait for sync
6. Check Firestore

**Expected Results:**
- ✅ All local changes upload when online
- ✅ Steps upload successfully
- ✅ Firestore has all steps
- ✅ No data loss

---

## Verification Checklist

### Phase 3.3 (Pull Cursor)
- [ ] Routines pull from Firestore on app launch
- [ ] Only new/updated routines are pulled (cursor works)
- [ ] Merge logic works (last-write-wins)
- [ ] Local changes are preserved when newer
- [ ] Sync cursor is updated after pull
- [ ] No duplicate routines

### Phase 3.4 (Sync RoutineSteps)
- [ ] Steps upload with routines
- [ ] Steps upload on app launch
- [ ] Step updates work (no duplicates)
- [ ] Step deletions are synced (soft delete)
- [ ] Steps sync across devices
- [ ] Security rules allow step operations
- [ ] Step order is preserved (orderIndex)

### Combined
- [ ] Full bidirectional sync works
- [ ] Offline → online sync works
- [ ] No data loss
- [ ] No duplicates
- [ ] Performance is acceptable

---

## Logs to Monitor

### Successful Pull (Phase 3.3)
```
🔄 Starting pull of routines from Firestore
📥 Last sync timestamp: [timestamp]
📥 Found X routine(s) to pull from Firestore
✅ Merged routine (remote wins): [id]
⏭️ Skipped routine (local wins): [id]
✅ Pull complete: merged X routine(s), skipped Y routine(s)
```

### Successful Step Upload (Phase 3.4)
```
🔄 Starting upload of unsynced steps for routine: [routineId]
📤 Found X unsynced step(s) to upload
✅ Uploaded step: [stepId]
✅ Marked X step(s) as synced
```

### Errors to Watch For
- ❌ "Missing or insufficient permissions" (security rules issue)
- ❌ "The query requires an index" (index missing)
- ❌ Duplicate steps/routines
- ❌ Steps not appearing in Firestore

---

## Firestore Console Verification

### Check Routines Collection
1. Go to Firestore Console
2. Navigate to `/routines` collection
3. Verify routines have:
   - `userId` field
   - `familyId` field (optional)
   - `updatedAt` timestamp

### Check Steps Subcollection
1. Open a routine document
2. Navigate to `steps` subcollection
3. Verify steps have:
   - `id`, `routineId`, `orderIndex`
   - `label`, `iconName` (optional)
   - `createdAt`, `deletedAt` (optional)

---

## Success Criteria

✅ **Phase 3.3 Complete When:**
- Routines pull from Firestore correctly
- Merge logic works (last-write-wins)
- Cursor tracking works
- No duplicates

✅ **Phase 3.4 Complete When:**
- Steps upload with routines
- Steps sync across devices
- Step updates work correctly
- No duplicates on edit

✅ **Both Phases Complete When:**
- All tests pass
- No errors in logs
- Data syncs correctly across devices
- Performance is acceptable

---

## Next Steps After Testing

Once Phase 3.3 and 3.4 are confirmed complete:
- [ ] Document any issues found
- [ ] Mark phases as complete in spec
- [ ] **Phase 3.5:** Use [PHASE_3_5_iOS_TESTING.md](./PHASE_3_5_iOS_TESTING.md) to test Sync RoutineAssignments
- [ ] Phase 3.6 (Sync CompletionEvents) – when ready
