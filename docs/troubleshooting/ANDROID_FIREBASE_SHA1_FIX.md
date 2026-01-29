# Fixing Firebase "Unknown calling package name" Error on Android

## Problem

You're seeing this error in Logcat:
```
java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'.
```

This is a **non-fatal** error, but it can prevent some Firebase features from working properly.

## Root Cause

This error occurs when your app's **SHA-1 fingerprint** is not registered in Firebase Console. Google Play Services uses this fingerprint to verify your app's identity.

## Solution: Register SHA-1 Fingerprint in Firebase Console

### Step 1: Get Your App's SHA-1 Fingerprint

#### Method 1: Using Android Studio – printSha1 task ⭐ **RECOMMENDED**

1. Open your project in **Android Studio**
2. Open the **Gradle** panel (right side, or View → Tool Windows → Gradle)
3. Navigate to: **app** → **Tasks** → **help** → **printSha1**
4. Double-click **printSha1** to run it
5. Look at the **Run** panel at the bottom
6. Find the line that says **SHA1:** and copy that value (e.g. `AB:CD:EF:...`)

#### Method 2: Using keytool with Android Studio’s JDK (macOS)

Android Studio ships with a JDK, so you can use its `keytool` without installing Java:

```bash
# Use Android Studio’s bundled JDK (adjust path if your install is different)
/Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

To show only the SHA-1 line:

```bash
/Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

#### Method 3: Using keytool (if Java is on your PATH)

**On macOS, if Java is not in PATH:**
```bash
# Find Java location first
/usr/libexec/java_home -V

# Then use the full path (replace X.X.X with your Java version)
/usr/libexec/java_home -v X.X.X --exec keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Or if Java is in PATH:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Extract just the SHA-1:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

#### Method 4: Using Gradle Wrapper (if gradlew exists)

```bash
cd android
./gradlew :app:printSha1
```

Look for the SHA-1 value in the output.

#### For Release Builds (Production):
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your-key-alias
```

### Step 2: Add SHA-1 to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click the **⚙️ Settings** icon (gear) → **Project settings**
4. Scroll down to **Your apps** section
5. Find your Android app (`com.HammersTech.RoutineChart`)
6. Click **Add fingerprint**
7. Paste your SHA-1 fingerprint
8. Click **Save**

### Step 3: Download Updated google-services.json

1. In Firebase Console, still in **Project settings**
2. Under **Your apps**, find your Android app
3. Click **Download google-services.json**
4. Replace the file at `android/app/google-services.json`
5. **Rebuild** your app

### Step 4: Verify Fix

1. Clean and rebuild:
   ```bash
   cd android
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. Run the app and check Logcat
3. The error should be gone (or at least not appear as frequently)

## Quick Reference: Get SHA-1 on macOS

### Easiest Method (Android Studio):
1. Gradle panel → `signingReport` task
2. Check Run panel output

### Command Line (if Java is available):
```bash
# Find Java first
/usr/libexec/java_home -V

# Use full path (replace version)
/usr/libexec/java_home -v 17 --exec keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# Output will show:
# SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
```

### If Java Not Found:
Install Java JDK or use Android Studio's built-in method (Method 1 above).

## Why This Happens

- **Debug builds** use a default debug keystore that Android Studio creates
- **Release builds** use your own signing key
- Firebase needs to know **both** SHA-1 fingerprints if you're testing with both debug and release builds
- Each developer machine has a different debug keystore, so each developer needs to register their SHA-1

## Current Workaround

The app now handles this error gracefully:
- The error is caught and logged (non-fatal)
- Firebase features may still work (depending on which features you're using)
- The app continues to run normally

However, **you should still register the SHA-1** for full Firebase functionality.

## Testing After Fix

1. Clear app data
2. Launch app
3. Sign in
4. Check Logcat - the error should be gone
5. Verify Firebase features work (Firestore sync, Auth, etc.)

## Multiple SHA-1 Fingerprints

You can register **multiple** SHA-1 fingerprints in Firebase Console:
- One for debug builds (development)
- One for release builds (production)
- One for each developer's debug keystore (if sharing the same Firebase project)

This is useful when:
- Multiple developers are working on the same project
- You have both debug and release builds
- You're using CI/CD with different signing keys

## Related Issues

If you still see issues after registering SHA-1:
- Check that `google-services.json` is in the correct location: `android/app/google-services.json`
- Verify the package name matches: `com.HammersTech.RoutineChart`
- Ensure the Firebase plugin is enabled in `build.gradle.kts`: `id("com.google.gms.google-services")`
- Check that you've synced Gradle after updating `google-services.json`
