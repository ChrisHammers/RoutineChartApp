# Firestore Security Rules Setup

## Quick Setup for Development/Testing

To enable Firestore sync, you need to configure Firestore security rules. For development/testing, you can use permissive rules, but **DO NOT use these in production**.

### Step 1: Go to Firebase Console
1. Open [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Firestore Database** → **Rules**

### Step 2: Add Development Rules

Copy and paste these rules for development/testing:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - anyone authenticated can read/write their own user document
    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Families collection - anyone authenticated can read/write their family's data
    match /families/{familyId} {
      allow read, write: if request.auth != null;
      
      // Invites subcollection
      match /invites/{inviteId} {
        allow read, write: if request.auth != null;
      }
    }
  }
}
```

### Step 3: Publish Rules
1. Click **Publish**
2. Rules will be active within a few seconds

## Important Notes

⚠️ **These rules are permissive and only for development/testing!**

For production, you'll need more restrictive rules that:
- Verify users belong to the family they're accessing
- Restrict invite access to family members only
- Add proper validation for data structure

## Testing After Rules Are Set

1. Sign in/sign up as a parent
2. Check Xcode console for sync messages:
   - ✅ `[Firestore Sync] User synced: {userId}`
   - ❌ `[Firestore Sync Error]` messages indicate problems
3. Check Firestore Console → `users` collection should now have documents
4. Generate an invite and check `families/{familyId}/invites/{inviteId}`

## Troubleshooting

If you still see errors:
1. Check Firebase Console → Authentication → Users (make sure user exists)
2. Verify `GoogleService-Info.plist` is in your Xcode project
3. Check Xcode console for detailed error messages
4. Ensure Firebase is initialized before any Firestore operations

