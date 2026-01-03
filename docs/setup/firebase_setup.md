# Firebase Setup Guide

This guide walks through setting up Firebase for the Routine Chart App.

---

## Prerequisites

- Google account
- Firebase CLI installed: `npm install -g firebase-tools`
- iOS and Android projects created

---

## 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name: `routine-chart-app`
4. Disable Google Analytics (optional for V1)
5. Click "Create project"

---

## 2. Enable Authentication

1. In Firebase Console → Authentication
2. Click "Get started"
3. Enable **Email/Password** provider
4. Save

**Note:** Child accounts may not have email addresses. Store username in displayName field.

---

## 3. Create Firestore Database

1. In Firebase Console → Firestore Database
2. Click "Create database"
3. Choose **Production mode**
4. Select region (e.g., `us-central`)
5. Click "Enable"

### Initial Security Rules

Upload the rules from `backend/firestore.rules` (we'll create this shortly).

Temporary dev rules (replace immediately):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 4. Add iOS App

1. In Firebase Console → Project settings
2. Click iOS icon under "Your apps"
3. Enter Bundle ID: `com.routinechart.RoutineChart`
4. Enter App nickname: `Routine Chart iOS`
5. Click "Register app"
6. Download `GoogleService-Info.plist`
7. Add to `ios/RoutineChart/RoutineChart/` directory
8. Follow Firebase iOS SDK setup instructions
9. Click "Continue to console"

---

## 5. Add Android App

1. In Firebase Console → Project settings
2. Click Android icon under "Your apps"
3. Enter Package name: `com.routinechart`
4. Enter App nickname: `Routine Chart Android`
5. Click "Register app"
6. Download `google-services.json`
7. Place in `android/app/` directory
8. Follow Firebase Android SDK setup instructions
9. Click "Continue to console"

---

## 6. Create Indexes

Firestore requires composite indexes for complex queries.

### Create via Console

1. Firestore → Indexes
2. Add these composite indexes:

**Events by family and time:**
- Collection ID: `events`
- Fields indexed:
  - `familyId` (Ascending)
  - `eventAt` (Ascending)
- Query scope: Collection

**Events by family, child, and time:**
- Collection ID: `events`
- Fields indexed:
  - `familyId` (Ascending)
  - `childId` (Ascending)
  - `eventAt` (Ascending)
- Query scope: Collection

**Events by family, day, and time:**
- Collection ID: `events`
- Fields indexed:
  - `familyId` (Ascending)
  - `localDayKey` (Ascending)
  - `eventAt` (Ascending)
- Query scope: Collection

### Or Deploy via CLI

```bash
cd backend
firebase deploy --only firestore:indexes
```

Uses `backend/firestore.indexes.json` file.

---

## 7. Setup Cloud Functions

### Initialize Functions

```bash
cd backend
firebase init functions
```

Choose:
- Language: **TypeScript**
- ESLint: **Yes**
- Install dependencies: **Yes**

### Deploy Functions

```bash
cd backend/functions
npm install
npm run build
firebase deploy --only functions
```

---

## 8. Configure Environment

### Development Environment

Create `.env.local` files for local development:

**iOS (optional):**
```
# ios/RoutineChart/.env.local
FIREBASE_USE_EMULATOR=true
FIREBASE_EMULATOR_HOST=localhost
```

**Android (optional):**
```
# android/local.properties
firebase.use.emulator=true
firebase.emulator.host=localhost
```

### Firebase Emulator (Development)

Use emulators for local development:

```bash
firebase emulators:start
```

Emulates:
- Authentication
- Firestore
- Cloud Functions

Update app to use emulators in debug builds:

**iOS:**
```swift
#if DEBUG
let settings = Firestore.firestore().settings
settings.host = "localhost:8080"
settings.isSSLEnabled = false
Firestore.firestore().settings = settings
#endif
```

**Android:**
```kotlin
if (BuildConfig.DEBUG) {
    Firebase.firestore.useEmulator("10.0.2.2", 8080)
    Firebase.auth.useEmulator("10.0.2.2", 9099)
}
```

---

## 9. Security Rules Deployment

### Deploy Firestore Rules

```bash
cd backend
firebase deploy --only firestore:rules
```

### Test Rules

Use the Rules Playground in Firebase Console:
1. Firestore → Rules
2. Click "Rules Playground"
3. Test read/write operations with different auth states

---

## 10. Monitoring & Logs

### Cloud Functions Logs

```bash
firebase functions:log
```

Or view in Firebase Console → Functions → Logs

### Firestore Usage

Monitor in Firebase Console → Firestore → Usage tab

**Free Tier Limits:**
- 50K reads/day
- 20K writes/day
- 20K deletes/day
- 1GB storage

**Blaze (Pay-as-you-go):**
- $0.06 per 100K reads
- $0.18 per 100K writes
- $0.02 per 100K deletes

---

## 11. Backup Strategy

### Automated Backups (Blaze plan only)

Enable automated backups in Firebase Console:
1. Firestore → Backups
2. Click "Get started"
3. Configure retention period

### Manual Export

```bash
gcloud firestore export gs://[BUCKET_NAME]/[EXPORT_FOLDER]
```

---

## 12. Troubleshooting

### "Permission denied" errors

**Check:**
- Security rules are deployed
- User is authenticated (`request.auth != null`)
- User has access to family data

**Debug:**
```bash
firebase firestore:rules:get
```

### "Index required" errors

Firestore will provide a link to create the required index. Click the link or:

1. Copy the index configuration from the error
2. Add to `firestore.indexes.json`
3. Deploy: `firebase deploy --only firestore:indexes`

### Cloud Functions not triggering

**Check:**
- Functions are deployed: `firebase deploy --only functions`
- Function logs for errors: `firebase functions:log`
- Event trigger matches document path exactly

---

## 13. Production Checklist

Before launching:

- [ ] Replace dev security rules with production rules
- [ ] Enable Cloud Firestore backups
- [ ] Setup monitoring alerts
- [ ] Review pricing quotas
- [ ] Test all security rules with Rules Playground
- [ ] Remove emulator configuration from production builds
- [ ] Enable App Check (optional, for bot protection)
- [ ] Configure OAuth providers if needed
- [ ] Review and optimize Firestore indexes
- [ ] Test on real devices with production Firebase project

---

## Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Security Rules Guide](https://firebase.google.com/docs/firestore/security/get-started)
- [Cloud Functions Documentation](https://firebase.google.com/docs/functions)
- [Firebase CLI Reference](https://firebase.google.com/docs/cli)

---

**Last Updated:** 2026-01-03

