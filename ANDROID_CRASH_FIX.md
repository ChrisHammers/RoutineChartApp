# 🔧 Android Crash Fix - NoClassDefFoundError

## Problem
The app crashes at launch with:
```
java.lang.NoClassDefFoundError: Failed resolution of: Lcom/HammersTech/RoutineChart/features/auth/child/ChildSignInViewModel_HiltModules_KeyModule_ProvideFactory
```

## Root Cause
- **Hilt annotation processor** didn't generate required classes
- **Package name mismatch**: Old app uses `com.routinechart`, new code uses `com.HammersTech.RoutineChart`
- Stale build artifacts

---

## 🚀 Quick Fix (RECOMMENDED)

### Option 1: Run the Fix Script
```bash
cd /Users/christopherhammers/Documents/GitHub/RoutineChartApp/android
./FIX_ANDROID_BUILD.sh
```

### Option 2: Manual Steps

1. **Uninstall ALL versions of the app:**
   ```bash
   adb uninstall com.routinechart
   adb uninstall com.HammersTech.RoutineChart
   ```

2. **Clean the project:**
   ```bash
   cd /Users/christopherhammers/Documents/GitHub/RoutineChartApp/android
   ./gradlew clean
   rm -rf app/build
   rm -rf build
   rm -rf .gradle
   ```

3. **Rebuild and install:**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

4. **Launch the app** from the emulator

---

## 🏥 If Still Crashing

### In Android Studio:

1. **Invalidate Caches:**
   - File → Invalidate Caches...
   - Check all boxes
   - Click "Invalidate and Restart"

2. **After restart, sync Gradle:**
   - File → Sync Project with Gradle Files

3. **Clean and Rebuild:**
   - Build → Clean Project
   - Build → Rebuild Project

4. **Run the app:**
   - Run → Run 'app'

---

## 📊 Verify the Fix

After the app launches, check Logcat for:

```
✅ EXPECTED LOGS:
MainActivity: onCreate started
MainViewModel: Auth state initialized. Current user: null
MainScreen: Auth user state: null
MainScreen: Showing auth flow

❌ If you see any ClassNotFoundException or NoClassDefFoundError:
- The build is still using stale artifacts
- Try the "Invalidate Caches" step above
- Or delete the entire android/.gradle folder
```

---

## 🎯 What Should Happen

1. App launches ✅
2. No crash ✅
3. You see one of:
   - **Auth selection screen** (Who are you? Parent/Child)
   - **OR Main app** (if you were previously signed in)

If you see the main app and want to test auth:
- Tap the **logout icon** (top-right corner of Parent Dashboard)
- You'll be taken to the auth flow

---

## 🔍 Understanding the Error

The error happened because:

1. **Hilt uses annotation processing (KSP)** to generate code at compile time
2. The generated code includes package names
3. When we changed package from `com.routinechart` → `com.HammersTech.RoutineChart`, Hilt needed to regenerate all files
4. Old build artifacts were cached
5. The app tried to load classes from the old package

The fix:
- **Clean build** = removes all cached artifacts
- **Uninstall** = removes old app with wrong package
- **Rebuild** = regenerates Hilt code with correct package

---

## 💡 Prevention

To avoid this in the future:
- When changing package names, **always** do a clean rebuild
- When Hilt errors occur, **first** try cleaning the build
- Use **Invalidate Caches** when things seem broken

---

## ✅ Success Checklist

- [ ] Uninstalled both app packages
- [ ] Cleaned build directories
- [ ] Rebuilt the project
- [ ] App launches without crashing
- [ ] Can see either auth screen OR main content
- [ ] No ClassNotFoundException in logs

---

## 🆘 Still Need Help?

If the app still crashes after following ALL steps:

1. **Check the package name in `build.gradle.kts`:**
   ```kotlin
   applicationId = "com.HammersTech.RoutineChart"  // Should be this
   namespace = "com.HammersTech.RoutineChart"      // Should be this
   ```

2. **Verify google-services.json:**
   - Location: `android/app/google-services.json`
   - Open it and check `package_name` matches `com.HammersTech.RoutineChart`

3. **Capture full logs:**
   ```bash
   adb logcat > crash_logs.txt
   ```
   Send the `crash_logs.txt` file

---

**Quick reminder:** iOS works fine, this is Android-only. The fix is straightforward - just need a complete clean rebuild! 🚀

