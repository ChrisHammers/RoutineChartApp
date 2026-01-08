# Phase 2.3: User Linking & Firestore Sync Specification

## Overview
Complete the family joining flow by actually linking authenticated users to families and syncing data to Firestore.

---

## User Stories

### Joining User
1. As a user, I want to be automatically added to the family after scanning a valid QR code
2. As a user, I want my account to be linked to the family I joined
3. As a user, I want to see the family's routines after joining

### Parent (Family Creator)
1. As a parent, I want to see who has joined my family
2. As a parent, I want invites to sync across my devices
3. As a parent, I want to know when someone uses my invite

---

## Technical Design

### User Linking Flow

#### After Valid Invite Scan
1. User scans QR code → Validates invite
2. **NEW**: Check if user is authenticated
   - If not authenticated → Prompt sign in/sign up
   - If authenticated → Continue
3. **NEW**: Link user to family
   - Update `User.familyId` = invite.familyId
   - If User doesn't exist → Create User record
   - If User has different familyId → Handle family switching
4. **NEW**: Increment invite `usedCount`
5. **NEW**: Sync to Firestore
   - Update User document
   - Update FamilyInvite document
6. Show success message
7. Navigate to family dashboard

### Domain Model Updates

#### User Model
```swift
struct User {
    let id: String              // Firebase Auth UID
    let familyId: String        // ← Update this when joining
    let role: Role              // parent | child
    let displayName: String
    let email: String?
    let createdAt: Date
}
```

#### FamilyInvite Model
- Already has `usedCount` field
- Need to track which users used it (optional, for analytics)

### Firestore Collections

#### `/families/{familyId}/invites/{inviteId}`
```json
{
  "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
  "familyId": "family123",
  "token": "secure_token_here",
  "inviteCode": "ABC-1234",
  "createdBy": "user123",
  "createdAt": "2026-01-05T10:00:00Z",
  "expiresAt": "2026-01-06T10:00:00Z",
  "maxUses": null,
  "usedCount": 0,
  "isActive": true
}
```

#### `/users/{userId}`
```json
{
  "id": "user123",
  "familyId": "family123",  // ← Update when joining
  "role": "child",
  "displayName": "Emma",
  "email": "emma@example.com",
  "createdAt": "2026-01-05T10:00:00Z"
}
```

---

## Implementation Plan

### Phase 2.3.1: User Linking (Local)
- [ ] Update `ScanInviteViewModel.joinFamily()` to:
  - Get current authenticated user ID (replace placeholder)
  - Update `User.familyId` in local database
  - Create User record if doesn't exist
  - Handle family switching (if user already in different family)
- [ ] Update `UserRepository` with:
  - `updateFamilyId(userId:familyId:)` method
  - `getCurrentUser()` method (from Firebase Auth)
- [ ] Update join flow to navigate to family dashboard after success

### Phase 2.3.2: Replace Placeholder User IDs
- [ ] Update `GenerateInviteViewModel` to use actual Firebase Auth user ID
- [ ] Update `ScanInviteViewModel` to use actual Firebase Auth user ID
- [ ] Remove all `"currentUserId"` placeholders

### Phase 2.3.3: Firestore Sync (Invites)
- [ ] Create `FirestoreFamilyInviteRepository`
- [ ] Implement sync methods:
  - `syncToFirestore(invite:)`
  - `syncFromFirestore(familyId:)`
- [ ] Update invite creation to sync to Firestore
- [ ] Update invite usage to sync to Firestore
- [ ] Handle offline/online scenarios

### Phase 2.3.4: Firestore Sync (Users)
- [ ] Create `FirestoreUserRepository`
- [ ] Implement sync methods:
  - `syncToFirestore(user:)`
  - `syncFromFirestore(userId:)`
- [ ] Update user linking to sync to Firestore
- [ ] Handle offline/online scenarios

### Phase 2.3.5: Real-time Updates
- [x] Listen to Firestore invite updates
- [x] Auto-dismiss expired invites in UI
- [x] Show real-time invite usage count

---

## Flow Diagrams

### Join Flow (Updated)
```
┌─────────────────────────┐
│  Scan QR Code          │
│  ✅ Valid Invite        │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Check Auth State       │
│  ┌───────────────────┐ │
│  │ Not Authenticated │ │
│  │ → Sign In/Up      │ │
│  └───────────────────┘ │
│  ┌───────────────────┐ │
│  │ Authenticated     │ │
│  │ → Continue        │ │
│  └───────────────────┘ │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Link User to Family    │
│  • Update User.familyId │
│  • Create User if needed│
│  • Handle family switch │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Increment usedCount    │
│  • Local DB             │
│  • Firestore            │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Sync to Firestore      │
│  • User document        │
│  • Invite document      │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Success!               │
│  Navigate to Dashboard  │
└─────────────────────────┘
```

---

## Database Changes

### User Table
- `familyId` field already exists
- Need to update it when joining

### FamilyInvite Table
- `usedCount` field already exists
- Need to increment on join

---

## Security Rules

### Firestore Security Rules (Phase 2.3)
```javascript
// Users can only read/write their own user document
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}

// Users can read invites for their family
match /families/{familyId}/invites/{inviteId} {
  allow read: if request.auth != null && 
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.familyId == familyId;
  
  // Only family members can create invites
  allow create: if request.auth != null && 
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.familyId == familyId &&
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'parent';
}
```

---

## Testing Checklist

### User Linking
- [ ] New user joins family → User record created with correct familyId
- [ ] Existing user joins different family → familyId updated
- [ ] User already in same family → No duplicate join
- [ ] Offline join → Syncs when online

### Firestore Sync
- [ ] Invite creation syncs to Firestore
- [ ] Invite usage syncs to Firestore
- [ ] User linking syncs to Firestore
- [ ] Offline changes sync when online
- [ ] Real-time updates work

### Error Handling
- [ ] Network errors handled gracefully
- [ ] Firestore permission errors handled
- [ ] Invalid user state handled

---

## Success Criteria

✅ User is linked to family after joining  
✅ User data syncs to Firestore  
✅ Invite data syncs to Firestore  
✅ Real-time updates work  
✅ Offline-first architecture maintained  
✅ No placeholder user IDs remain  

---

## Next Steps

1. Start with Phase 2.3.1: User Linking (Local)
2. Then Phase 2.3.2: Replace Placeholder User IDs
3. Then Phase 2.3.3-2.3.4: Firestore Sync
4. Finally Phase 2.3.5: Real-time Updates

