# Phase 3.3 & 3.4 Android Testing Checklist

**Status:** ✅ READY FOR TESTING  
**Phases:** 3.3 (Pull Cursor - Routines) & 3.4 (Sync RoutineSteps - Upload & Pull)

---

## How to Monitor Logs on Android

**Using Android Studio Logcat:**
1. Open Android Studio
2. Connect your device or start an emulator
3. Open Logcat (View → Tool Windows → Logcat)
4. Filter by tag: `AppLogger` or search for specific log messages
5. Filter by log level: `Info` or `Error` to see relevant messages

**Using ADB:**
```bash
adb logcat | grep -E "AppLogger|Starting pull|Pull complete|Uploaded step"
```

---

## Phase 3.3: Pull Cursor (Routines) - Testing

### Test 1: Initial Pull (No Cursor)
**Objective:** Verify routines are pulled from Firestore when no cursor exists

**Steps:**
1. Clear app data: Settings → Apps → RoutineChart → Storage → Clear Data
   - OR uninstall and reinstall the app
2. Ensure you have routines in Firestore (created from another device or manually in Firestore Console)
3. Launch the app and sign in
4. Navigate to Parent Dashboard
5. Check Logcat for: `🔄 Starting pull of routines from Firestore`
6. Verify: Routines appear in the dashboard

**Expected Results:**
- ✅ Routines from Firestore appear in the app
- ✅ Log shows: `📥 Found X routine(s) to pull from Firestore`
- ✅ Log shows: `✅ Pull complete: merged X routine(s), skipped Y routine(s)`
- ✅ Sync cursor is created/updated

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Starting pull\|Found.*routine\|Pull complete"
```

---

### Test 2: Incremental Pull (With Cursor)
**Objective:** Verify only new/updated routines are pulled

**Steps:**
1. Have routines already synced locally (from Test 1)
2. Create or update a routine on another device (or in Firestore Console)
   - Change the title or add a new routine
3. Wait a few seconds (5-10 seconds)
4. Close and relaunch the app (or navigate away and back to Parent Dashboard)
5. Check Logcat for cursor timestamp

**Expected Results:**
- ✅ Only new/updated routines are pulled
- ✅ Log shows: `📥 Last sync timestamp: [previous timestamp]`
- ✅ Log shows: `📥 Found X routine(s) to pull` (only new/updated ones)
- ✅ Existing routines are not duplicated
- ✅ New/updated routines appear in the dashboard

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Last sync timestamp\|Found.*routine"
```

---

### Test 3: Merge Logic (Last-Write-Wins)
**Objective:** Verify remote changes overwrite local when remote is newer

**Steps:**
1. Create a routine locally (note the title, e.g., "Morning Routine")
2. Wait for it to upload to Firestore (check Logcat for upload confirmation)
3. Update the routine title in Firestore Console (or another device)
   - Change title to "Updated Morning Routine"
   - Update the `updatedAt` field to current timestamp
4. Wait a few seconds (5-10 seconds)
5. Close and relaunch the app (or navigate away and back to Parent Dashboard)
6. Check the routine title in the app

**Expected Results:**
- ✅ Local routine title is updated to match Firestore ("Updated Morning Routine")
- ✅ Log shows: `✅ Merged routine (remote wins): [routineId]`
- ✅ Log shows timestamp comparison: `remote: [newer], local: [older]`

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Merged routine\|remote wins"
```

---

### Test 4: Local Wins (Skip Remote)
**Objective:** Verify local changes are preserved when local is newer

**Steps:**
1. Create a routine locally (e.g., "Local Routine")
2. Update it locally (change title to "Local Updated")
3. Before it uploads, update the same routine in Firestore Console
   - Change title to "Remote Updated"
   - Set `updatedAt` to an older timestamp (e.g., 1 hour ago)
4. Launch the app to trigger pull
5. Check the routine title

**Expected Results:**
- ✅ Local routine title is preserved ("Local Updated", not overwritten)
- ✅ Log shows: `⏭️ Skipped routine (local wins): [routineId]`
- ✅ Log shows timestamp comparison: `remote: [older], local: [newer]`
- ✅ Local changes will upload via upload queue

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Skipped routine\|local wins"
```

---

### Test 5: Pull on App Launch
**Objective:** Verify pull happens automatically on app launch

**Steps:**
1. Create a routine on another device (or in Firestore Console)
2. Close the app completely (swipe away from recent apps)
3. Launch the app
4. Sign in if needed
5. Navigate to Parent Dashboard
6. Check Logcat

**Expected Results:**
- ✅ Pull is triggered automatically on app launch
- ✅ Log shows: `🔄 Starting pull of routines from Firestore`
- ✅ Log shows: `✅ Pull complete: merged X routine(s)`
- ✅ New routines appear without manual refresh

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Starting pull\|Pull complete"
```

---

## Phase 3.4: Sync RoutineSteps - Testing

### Test 6: Steps Upload with Routine
**Objective:** Verify steps are uploaded when routine is uploaded

**Steps:**
1. Create a new routine with 3-4 steps:
   - Title: "Test Routine"
   - Step 1: "Wake up"
   - Step 2: "Brush teeth"
   - Step 3: "Get dressed"
   - Step 4: "Eat breakfast"
2. Save the routine
3. Check Logcat for step upload messages
4. Wait 5-10 seconds
5. Verify in Firestore Console: Navigate to `/routines/{routineId}/steps/{stepId}`

**Expected Results:**
- ✅ Routine uploads successfully
- ✅ Log shows: `🔄 Starting upload of unsynced steps for routine: [routineId]`
- ✅ Log shows: `📤 Found X unsynced step(s) to upload for routine: [routineId]`
- ✅ Log shows: `✅ Uploaded step: [stepId]` for each step
- ✅ Steps appear in Firestore under the routine's subcollection
- ✅ All steps are marked as synced

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Starting upload\|Found.*unsynced step\|Uploaded step"
```

**Firestore Console Check:**
1. Go to Firebase Console → Firestore Database
2. Navigate to `routines` collection
3. Find your routine document
4. Click on the `steps` subcollection
5. Verify all 4 steps are present with correct `label`, `orderIndex`, etc.

---

### Test 7: Steps Upload on App Launch
**Objective:** Verify unsynced steps are uploaded on app launch

**Steps:**
1. Create a routine with steps while offline (airplane mode)
   - OR create steps and immediately close the app before upload completes
2. Close the app completely
3. Go online (disable airplane mode)
4. Launch the app (while online)
5. Navigate to Parent Dashboard
6. Check Logcat

**Expected Results:**
- ✅ Routine uploads
- ✅ Steps upload after routine uploads
- ✅ Log shows: `✅ Uploaded X step(s) for routine: [routineId]`
- ✅ Steps appear in Firestore Console

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Starting upload\|Uploaded step"
```

---

### Test 8: Edit Routine - Update Steps
**Objective:** Verify step updates are synced (no duplicates)

**Steps:**
1. Edit an existing routine (tap on it in Parent Dashboard)
2. Modify an existing step:
   - Change Step 1 label from "Wake up" to "Wake up early"
3. Add a new step:
   - Add "Make bed" as a new step
4. Remove a step:
   - Delete Step 2 ("Brush teeth")
5. Save the routine
6. Check Logcat and Firestore Console

**Expected Results:**
- ✅ Modified steps are updated (not duplicated)
- ✅ New steps are created
- ✅ Removed steps are soft-deleted (`deletedAt` set in Firestore)
- ✅ Log shows step uploads
- ✅ Firestore reflects all changes
- ✅ No duplicate steps in Firestore

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Uploaded step\|Marked.*as synced"
```

**Firestore Console Check:**
1. Check the routine's `steps` subcollection
2. Verify:
   - Step 1 label is "Wake up early" (updated)
   - New step "Make bed" exists
   - Step 2 ("Brush teeth") has `deletedAt` field set
   - No duplicate steps

---

### Test 9: Steps Security Rules
**Objective:** Verify Firestore security rules allow step operations

**Steps:**
1. Create a routine with steps
2. Verify steps upload successfully (check Logcat)
3. Check Firestore Console for steps under `/routines/{routineId}/steps/`
4. Try to read steps from another authenticated account (if possible)

**Expected Results:**
- ✅ No "Missing or insufficient permissions" errors in Logcat
- ✅ Steps are created in Firestore
- ✅ Steps are readable by authenticated users in the same family
- ✅ Only routine owner/parent can modify steps

**Logcat Filter:**
```
tag:AppLogger level:ERROR | grep "permission\|insufficient"
```

---

### Test 10: Multi-Device Step Sync
**Objective:** Verify steps sync across devices

**Steps:**
1. **Device A (Android):** Create a routine with steps:
   - Title: "Multi-Device Test"
   - Step 1: "Step One"
   - Step 2: "Step Two"
   - Step 3: "Step Three"
2. Wait for upload to complete (check Logcat)
3. **Device B (iOS or another Android):** Launch app and sign in with same account
4. Navigate to Parent Dashboard
5. Verify steps appear on Device B

**Expected Results:**
- ✅ Steps appear on Device B
- ✅ Step order is preserved (`orderIndex` matches)
- ✅ Step labels and icons are correct
- ✅ All steps are present (no missing steps)

**Alternative (Single Device):**
- Use Firestore Console to verify steps exist, then clear app data and reinstall to simulate Device B

---

## Combined Tests

### Test 11: Full Sync Flow (Upload + Pull)
**Objective:** Verify complete bidirectional sync

**Steps:**
1. **Device A:** Create routine with steps:
   - Title: "Sync Test Routine"
   - Step 1: "Device A Step 1"
   - Step 2: "Device A Step 2"
2. Wait for upload (check Logcat)
3. **Device B:** Launch app (or clear data and reinstall)
4. Verify routine and steps appear on Device B
5. **Device B:** Edit routine:
   - Change title to "Sync Test Routine - Updated"
   - Modify Step 1: "Device B Updated Step 1"
   - Add Step 3: "Device B Step 3"
6. Wait for upload
7. **Device A:** Close and relaunch app (or navigate away and back)
8. Verify changes sync to Device A

**Expected Results:**
- ✅ Device B receives routine and steps from Device A
- ✅ Device A receives updated routine and steps from Device B
- ✅ No duplicates
- ✅ All changes are preserved
- ✅ Step order is maintained

**Logcat Filter (Device A):**
```
tag:AppLogger level:INFO | grep "Starting pull\|Merged routine\|Found.*step"
```

**Logcat Filter (Device B):**
```
tag:AppLogger level:INFO | grep "Starting upload\|Uploaded step"
```

---

### Test 12: Offline → Online Sync
**Objective:** Verify steps sync when going online

**Steps:**
1. Go offline (enable airplane mode)
2. Create a routine with steps:
   - Title: "Offline Routine"
   - Step 1: "Offline Step 1"
   - Step 2: "Offline Step 2"
3. Edit the routine (add/modify steps):
   - Add Step 3: "Offline Step 3"
   - Modify Step 1: "Offline Step 1 - Modified"
4. Go online (disable airplane mode)
5. Launch app or navigate to Parent Dashboard (triggers sync)
6. Wait 10-15 seconds
7. Check Firestore Console

**Expected Results:**
- ✅ All local changes upload when online
- ✅ Steps upload successfully
- ✅ Log shows: `✅ Uploaded X step(s) for routine: [routineId]`
- ✅ Firestore has all steps
- ✅ No data loss

**Logcat Filter:**
```
tag:AppLogger level:INFO | grep "Starting upload\|Uploaded step"
```

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
- [ ] Step order is preserved (`orderIndex`)

### Combined
- [ ] Full bidirectional sync works
- [ ] Offline → online sync works
- [ ] No data loss
- [ ] No duplicates
- [ ] Performance is acceptable (< 5 seconds for sync)

---

## Logs to Monitor

### Successful Pull (Phase 3.3)
```
🔄 Starting pull of routines from Firestore for userId: [userId], familyId: [familyId]
📥 Last sync timestamp: [timestamp]
📥 Found X routine(s) to pull from Firestore
✅ Merged routine (remote wins): [id]
⏭️ Skipped routine (local wins): [id]
✅ Pull complete: merged X routine(s), skipped Y routine(s)
```

### Successful Step Upload (Phase 3.4)
```
🔄 Starting upload of unsynced steps for routine: [routineId]
📤 Found X unsynced step(s) to upload for routine: [routineId]
✅ Uploaded step: [stepId]
✅ Marked X step(s) as synced
```

### Successful Step Pull (Phase 3.4)
```
📥 Found X step(s) in Firestore for routine: [routineId]
✅ Merged step (remote wins): [stepId]
⏭️ Skipped step (local wins): [stepId]
```

### Errors to Watch For
- ❌ `Missing or insufficient permissions` (security rules issue)
- ❌ `The query requires an index` (index missing - check Firestore Console)
- ❌ Duplicate steps/routines in Firestore
- ❌ Steps not appearing in Firestore
- ❌ `Failed to query Firestore` (network/connectivity issue)

---

## Firestore Console Verification

### Check Routines Collection
1. Go to Firebase Console → Firestore Database
2. Navigate to `routines` collection
3. Verify routines have:
   - `userId` field (String)
   - `familyId` field (String, optional)
   - `updatedAt` field (Timestamp)
   - `title`, `iconName`, etc.

### Check Steps Subcollection
1. Open a routine document
2. Navigate to `steps` subcollection
3. Verify steps have:
   - `id`, `routineId`, `orderIndex` (Number)
   - `label` (String), `iconName` (String, optional)
   - `createdAt` (Timestamp), `deletedAt` (Timestamp, optional, null if not deleted)

### Check Sync Cursor
1. Navigate to `sync_cursors` collection
2. Find document with `collection = "routines"`
3. Verify `lastSyncedAt` timestamp is recent

---

## Common Issues & Troubleshooting

### Issue: Routines not pulling
**Check:**
- [ ] User is signed in
- [ ] Network connection is active
- [ ] Firestore indexes are created (check Firebase Console)
- [ ] Security rules allow read access
- [ ] Logcat shows any errors

**Solution:**
- Check Logcat for error messages
- Verify Firestore indexes in Firebase Console → Firestore → Indexes
- Check security rules in Firebase Console → Firestore → Rules

### Issue: Steps not uploading
**Check:**
- [ ] Routine uploaded successfully first
- [ ] Steps are marked as `synced = false` in local database
- [ ] Network connection is active
- [ ] Security rules allow write access to `/routines/{routineId}/steps/{stepId}`

**Solution:**
- Check Logcat for upload messages
- Verify security rules allow writes to steps subcollection
- Check if routine exists in Firestore before steps can upload

### Issue: Duplicate steps
**Check:**
- [ ] Step IDs are unique
- [ ] `orderIndex` is correct
- [ ] Steps are not being created multiple times

**Solution:**
- Check Logcat for duplicate creation messages
- Verify step IDs in Firestore are unique
- Check if `save()` method is being called multiple times

### Issue: Steps not syncing across devices
**Check:**
- [ ] Both devices are signed in with same account
- [ ] Both devices have network connection
- [ ] Pull is triggered on Device B
- [ ] Steps exist in Firestore

**Solution:**
- Verify steps exist in Firestore Console
- Check if pull is happening on Device B (check Logcat)
- Ensure both devices are in the same family (if using `familyId`)

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
- Step deletions are synced

✅ **Both Phases Complete When:**
- All tests pass
- No errors in Logcat
- Data syncs correctly across devices
- Performance is acceptable (< 5 seconds for sync)
- Firestore Console shows correct data

---

## Next Steps After Testing

Once Phase 3.3 and 3.4 are confirmed complete:
- [ ] Document any issues found
- [ ] Mark phases as complete in spec
- [ ] Move to Phase 3.5 (Sync RoutineAssignments) or Phase 3.6 (Sync CompletionEvents)
