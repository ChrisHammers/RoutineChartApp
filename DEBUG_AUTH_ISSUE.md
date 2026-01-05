# 🔍 Debugging Auth Flow Issue

## Problem
The app is not showing the auth flow screen even after clearing storage.

## Changes Made
I've added extensive logging to help diagnose the issue. The app will now log:
- Initial auth state when app starts
- Auth state changes
- Which screen is being shown (auth flow vs main content)

---

## 🧪 Step-by-Step Debugging

### Android

1. **Clear ALL app data completely:**
   ```bash
   cd /Users/christopherhammers/Documents/GitHub/RoutineChartApp/android
   adb shell pm clear com.HammersTech.RoutineChart
   ```

2. **Build and run the app:**
   ```bash
   ./gradlew installDebug
   adb logcat -c  # Clear logs
   adb logcat | grep -E "MainViewModel|MainScreen|RoutineChartApp|AUTH"
   ```

3. **Check the logs for these key messages:**
   ```
   MainViewModel: Auth state initialized. Current user: null  ← Should be null!
   MainScreen: Auth user state: null                           ← Should be null!
   MainScreen: Showing auth flow                               ← Should see this!
   ```

4. **If you see "Current user: User(...)" instead of null:**
   - Firebase has a cached anonymous user
   - Solution: Uninstall the app completely
   ```bash
   adb uninstall com.HammersTech.RoutineChart
   ```

### iOS

1. **Delete the app from simulator completely:**
   - Long press app icon → Delete App
   - OR in Terminal:
   ```bash
   xcrun simctl uninstall booted com.HammersTech.RoutineChart
   ```

2. **Clean build:**
   ```bash
   cd /Users/christopherhammers/Documents/GitHub/RoutineChartApp/ios/RoutineChart
   xcodebuild clean
   ```

3. **Build and run from Xcode**

4. **Check console logs for:**
   ```
   AppDependencies initialized. Initial auth user: nil  ← Should be nil!
   ContentView: Showing auth flow. User is nil          ← Should see this!
   ```

5. **If you see a user ID instead of nil:**
   - Reset simulator completely:
   ```bash
   xcrun simctl erase all
   ```

---

## 🎯 What to Look For

### ✅ CORRECT Behavior (what we want):
```
[Android Logs]
MainViewModel: Auth state initialized. Current user: null
MainScreen: Auth user state: null
MainScreen: Showing auth flow
← Auth selection screen should appear

[iOS Logs]
AppDependencies initialized. Initial auth user: nil
ContentView: Showing auth flow. User is nil
← Auth selection screen should appear
```

### ❌ INCORRECT Behavior (bug):
```
[Android Logs]
MainViewModel: Auth state initialized. Current user: User(id=abc123, ...)
MainScreen: Auth user state: User(id=abc123, email=null, anonymous=true)
MainScreen: Showing authenticated content
← Goes straight to main app (WRONG!)

[iOS Logs]
AppDependencies initialized. Initial auth user: abc123-def456
ContentView: Showing main content. User: abc123-def456
← Goes straight to main app (WRONG!)
```

---

## 🔧 Solutions Based on Logs

### Issue 1: "Current user: User(...anonymous=true)"
**Problem:** Firebase has an anonymous user cached from previous test

**Android Solution:**
```bash
# Complete uninstall
adb uninstall com.HammersTech.RoutineChart

# Clear Firebase cache
adb shell rm -rf /data/data/com.HammersTech.RoutineChart

# Reinstall
./gradlew installDebug
```

**iOS Solution:**
```bash
# Reset simulator
xcrun simctl erase all

# Rebuild
xcodebuild clean build
```

### Issue 2: "Current user: User(...email=...@...)"
**Problem:** You have a real authenticated user from previous testing

**Solution:** This is expected! Use the sign-out button:
- **Android**: Top-right corner of Parent Dashboard
- **iOS**: Top-left corner of Parent Dashboard

### Issue 3: Still not working after uninstall
**Problem:** Firebase might be persisting auth across installs (rare but possible)

**Android Solution:**
1. Go to Firebase Console
2. Authentication → Users
3. Delete all test users
4. Reinstall app

**iOS Solution:**
1. Check Keychain Access (macOS app)
2. Search for "firebase" or your app bundle ID
3. Delete any related entries
4. Reinstall app

---

## 🚨 Nuclear Option: Complete Reset

If nothing works, do a complete reset:

### Android
```bash
# Stop emulator
adb emu kill

# Delete all emulator data
rm -rf ~/.android/avd/*

# Recreate emulator in Android Studio
# Then reinstall app
```

### iOS
```bash
# Delete derived data
rm -rf ~/Library/Developer/Xcode/DerivedData

# Reset all simulators
xcrun simctl erase all

# Clean and rebuild
cd ios/RoutineChart
xcodebuild clean
# Then build from Xcode
```

---

## 📊 Expected Log Flow (Full Auth Cycle)

### 1. Fresh Install (No Auth)
```
App starts → Auth user: nil → Show AuthFlowScreen
User taps "I'm a Parent" → Show ParentSignInScreen
User enters email/password → Tap "Sign In"
Auth: User signed in with email: test@example.com
Auth user changed: abc123 → Show main content
```

### 2. App Restart (With Auth)
```
App starts → Auth user: abc123 → Show main content directly
(No auth screen - this is correct!)
```

### 3. Sign Out
```
User taps logout button → Auth: User signed out
Auth user changed: nil → Show AuthFlowScreen
```

---

## 🐛 Common Mistakes

### ❌ "I cleared app data but still see main content"
- App data ≠ uninstall
- Solution: Uninstall completely, not just clear data

### ❌ "I reset simulator but still logged in"
- Xcode might have cached the build
- Solution: Clean build folder (Cmd+Shift+K in Xcode)

### ❌ "Logs say 'null' but I see main content"
- UI state might be out of sync
- Solution: Check if `AuthFlowScreen()` is actually being called

---

## 📞 Still Not Working?

### Share These Logs:
1. **Android:**
   ```bash
   adb logcat | grep -E "MainViewModel|MainScreen|RoutineChartApp|AUTH" > android_auth_logs.txt
   ```

2. **iOS:**
   - In Xcode: View → Debug Area → Activate Console
   - Copy all logs that mention "Auth", "ContentView", or "AppDependencies"

3. **Check Firebase Console:**
   - Go to Firebase Console → Authentication → Users
   - Screenshot the users list
   - Check if there are any users (there shouldn't be if you cleared everything)

---

## ✅ Success Indicators

You'll know it's working when you see:

1. **On fresh install:** Auth selection screen immediately
2. **In logs:** "Current user: null" and "Showing auth flow"
3. **After sign-in:** Main content with your routines
4. **After sign-out:** Back to auth selection screen

Good luck debugging! 🔍

