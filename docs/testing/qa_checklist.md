# QA Testing Checklist

Manual testing checklist for Routine Chart App V1.

## Phase 0: Foundation

- [x] Android project builds successfully
- [x] iOS project builds successfully  
- [x] Firebase project configured
- [x] Documentation created and accessible
- [ ] CI workflows run successfully (requires Firebase secrets)

---

## Phase 1: Local Functionality (TBD)

### Family & User Creation
- [ ] Can create a new family
- [ ] Family settings save correctly (timezone, week start)
- [ ] Can add child profiles
- [ ] Child profiles save with correct ageBand and readingMode

### Routine Creation
- [ ] Parent can create a routine
- [ ] Can add steps to routine
- [ ] Steps can be reordered
- [ ] Routine saves with all steps
- [ ] Can edit routine title and steps
- [ ] Can soft-delete routine (deletedAt set)

### Routine Assignment
- [ ] Can assign routine to child
- [ ] Assignment persists across app restarts
- [ ] Can deactivate assignment (isActive = false)
- [ ] Deactivated routines don't show for child

### Completion Flow
- [ ] Child can tap step to mark complete
- [ ] Completion creates event with type "complete"
- [ ] Can tap completed step to undo
- [ ] Undo creates event with type "undo"
- [ ] Step visual state reflects completion correctly
- [ ] Routine shows complete when all steps done
- [ ] State persists across app restarts

### Event Ordering
- [ ] Events sorted by eventAt then eventId
- [ ] Last event type determines current state
- [ ] Multiple undo/complete toggles work correctly
- [ ] Day key uses family timezone

---

## Phase 2: Auth & QR Join (TBD)

### Parent Authentication
- [ ] Parent can sign up with email/password
- [ ] Parent can sign in with existing account
- [ ] Error messages show for invalid credentials
- [ ] Sign out works correctly
- [ ] Session persists across app restarts

### QR Invite Generation
- [ ] Parent can generate join token
- [ ] QR code displays correctly
- [ ] Token expires after 24 hours
- [ ] Can generate multiple tokens

### Child Join Flow
- [ ] Child can scan QR code
- [ ] Scanning parses token correctly
- [ ] Can create child account with token
- [ ] Child account links to correct family
- [ ] Expired tokens are rejected
- [ ] Used tokens are rejected
- [ ] Invalid tokens show error message

### Multi-Device
- [ ] Two devices can join same family
- [ ] Both devices see same routines (after sync)
- [ ] Parent on Device A sees child's completion from Device B

---

## Phase 3: Cloud Sync (TBD)

### Upload Queue
- [ ] Offline events queue locally
- [ ] Events upload when connectivity restored
- [ ] Sync indicator shows during upload
- [ ] Failed uploads retry automatically

### Pull & Merge
- [ ] Device A creates routine → appears on Device B
- [ ] Device A edits routine → changes appear on Device B
- [ ] Device B completes step → shows on Device A
- [ ] Last-write-wins works for routine edits
- [ ] Event log append-only (no updates/deletes)

### Offline Mode
- [ ] All operations work without network
- [ ] UI shows offline indicator
- [ ] Actions sync correctly when back online
- [ ] No data loss from offline usage

### Conflict Resolution
- [ ] Event ordering preserved across devices
- [ ] Day boundaries use family timezone
- [ ] Derived state consistent across devices

---

## Phase 4: Analytics (TBD)

### Days Practiced
- [ ] Counts distinct days with ≥1 completion
- [ ] Streak counts consecutive days
- [ ] Streak doesn't reset punitively
- [ ] Updates in real-time after completion

### Routine Completion Count
- [ ] Counts days where routine fully completed
- [ ] Per-child stats show correctly
- [ ] Per-routine stats show correctly
- [ ] Syncs across devices

### UI Presentation
- [ ] Calm, non-competitive design
- [ ] No scoring or points
- [ ] No comparisons between children
- [ ] Child sees only their own analytics
- [ ] Parent sees all children's analytics

---

## Phase 5: Pricing (TBD)

### Free Tier
- [ ] Can create up to 3 routines
- [ ] Blocked from creating 4th routine
- [ ] Paywall shows with clear message
- [ ] Cloud sync still works on free tier
- [ ] Existing routines remain accessible

### Paid Tier
- [ ] Can create unlimited routines
- [ ] No paywall shown
- [ ] Purchase state syncs across devices
- [ ] Receipt validation works (iOS)
- [ ] Billing verification works (Android)

### Server Enforcement
- [ ] Server soft-deletes over-limit routines
- [ ] planTier correctly stored in Firestore
- [ ] Purchase verified server-side

---

## Cross-Platform Compatibility

- [ ] iOS and Android devices in same family
- [ ] Events from iOS appear on Android
- [ ] Events from Android appear on iOS
- [ ] Enum values match exactly
- [ ] Timestamps in correct format

---

## Accessibility

### iOS VoiceOver
- [ ] All buttons have accessibility labels
- [ ] Routine steps announce correctly
- [ ] Completion state announced
- [ ] Navigation works with VoiceOver

### Android TalkBack
- [ ] All buttons have content descriptions
- [ ] Routine steps announce correctly
- [ ] Completion state announced
- [ ] Navigation works with TalkBack

### Visual
- [ ] Large tap targets (min 44x44pt / 48x48dp)
- [ ] High contrast mode works
- [ ] Dynamic type respected
- [ ] Colors meet WCAG AA contrast

---

## Performance

- [ ] App launches in < 2 seconds
- [ ] Routine list scrolls smoothly (60fps)
- [ ] No lag when completing steps
- [ ] Large event logs don't slow app
- [ ] Database queries optimized with indexes

---

## Edge Cases

### Time & Timezone
- [ ] Events at midnight use correct day key
- [ ] Timezone changes handled correctly
- [ ] DST transitions don't break day keys
- [ ] Multi-timezone families work correctly

### Data Limits
- [ ] 100+ events for single step
- [ ] 50+ steps in routine
- [ ] 10+ children in family
- [ ] 100+ routines (paid tier)

### Network Failures
- [ ] Graceful degradation during sync failures
- [ ] Retry logic works correctly
- [ ] No crashes from network errors
- [ ] Error messages user-friendly

---

## Security

- [ ] Only authenticated users access data
- [ ] Parents can't see other families' data
- [ ] Children can't edit routines
- [ ] Children can only create events for their childId
- [ ] Security rules enforced server-side
- [ ] No sensitive data in logs

---

**Last Updated:** 2026-01-03
**Phase:** 0 (Foundation)

