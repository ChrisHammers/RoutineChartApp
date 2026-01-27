# How to Clear App Data on Android

This guide shows multiple ways to clear app data for testing Phase 3.3 Test 1 (Initial Pull).

---

## Method 1: Settings App (Easiest - Works on All Devices)

### Steps:
1. Open **Settings** app on your Android device/emulator
2. Navigate to **Apps** (or **Apps & notifications** on older Android versions)
3. Find and tap **RoutineChart** (or search for it)
4. Tap **Storage** (or **Storage & cache**)
5. Tap **Clear data** (or **Clear storage**)
6. Confirm by tapping **OK** or **Delete**

### What This Does:
- Clears all app data including:
  - Local database (SQLite/Room)
  - SharedPreferences
  - Cache
  - Sync cursors
- **Does NOT** uninstall the app
- You'll need to sign in again after clearing

---

## Method 2: ADB Command Line (Fastest for Testing)

### Prerequisites:
- Android device connected via USB with USB debugging enabled
- OR Android emulator running
- ADB installed (comes with Android Studio)

### Steps:

1. **Find ADB:**
   - On macOS/Linux: Usually at `~/Library/Android/sdk/platform-tools/adb`
   - On Windows: Usually at `C:\Users\[YourName]\AppData\Local\Android\Sdk\platform-tools\adb.exe`
   - Or add it to your PATH

2. **Check if device is connected:**
   ```bash
   adb devices
   ```
   Should show your device/emulator listed.

3. **Clear app data:**
   ```bash
   adb shell pm clear com.HammersTech.RoutineChart
   ```

### What This Does:
- Same as Method 1, but faster
- No need to navigate through Settings
- Great for repeated testing

### Alternative: Clear specific data only
If you want to keep some data but clear the database:
```bash
# Clear only the database (more advanced)
adb shell run-as com.HammersTech.RoutineChart rm -rf databases/
```

---

## Method 3: Uninstall & Reinstall (Most Thorough)

### Steps:
1. Long-press the **RoutineChart** app icon
2. Tap **Uninstall** (or drag to Uninstall)
3. Confirm uninstallation
4. Reinstall from Android Studio or APK

### What This Does:
- Removes all app data completely
- Removes the app itself
- Requires reinstallation

---

## Method 4: Android Studio Device Manager (Emulator Only)

### Steps:
1. Open Android Studio
2. Go to **Tools → Device Manager**
3. Right-click on your emulator
4. Select **Wipe Data**
5. Confirm

### What This Does:
- Wipes the entire emulator (all apps and data)
- Only works for emulators
- More drastic - removes everything

---

## Method 5: Programmatic Reset (For Development)

You can also add a debug button in your app to clear data programmatically:

```kotlin
// In a debug/test screen
Button(onClick = {
    context.deleteDatabase("routine_chart_database")
    // Or clear SharedPreferences
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().apply()
}) {
    Text("Clear All Data (Debug)")
}
```

---

## Recommended Approach for Testing

### For Quick Testing (Method 2 - ADB):
```bash
# One command to clear and relaunch
adb shell pm clear com.HammersTech.RoutineChart && adb shell am start -n com.HammersTech.RoutineChart/.MainActivity
```

### For Manual Testing (Method 1 - Settings):
- Use Settings app when you want to verify the UI flow
- Good for understanding what users would experience

---

## Verifying Data is Cleared

After clearing app data, verify:

1. **Launch the app** - you should see the sign-in screen (not logged in)
2. **Check Logcat** for:
   ```
   No cursor found - starting fresh pull
   ```
   or
   ```
   Last sync timestamp: 1970-01-01 (epoch 0)
   ```
3. **Check local database** (if you have database inspector):
   - Should be empty or only have seed data after sign-in

---

## Package Name

The app's package name is: `com.HammersTech.RoutineChart`

Use this in ADB commands or when searching in Settings.

---

## Troubleshooting

### "Device not found" (ADB)
- Enable USB debugging on your device
- Check USB connection
- Run `adb kill-server && adb start-server`

### "Permission denied" (ADB)
- Make sure USB debugging is enabled
- Accept the RSA key prompt on your device
- Try `adb root` (requires rooted device)

### App still has data after clearing
- Make sure you cleared **Storage/Data**, not just **Cache**
- Some apps store data in external storage - check `/sdcard/Android/data/com.HammersTech.RoutineChart/`

---

## Quick Reference

| Method | Speed | Ease | Use Case |
|--------|-------|------|----------|
| Settings App | Slow | Easy | Manual testing, UI verification |
| ADB Command | Fast | Medium | Repeated testing, automation |
| Uninstall/Reinstall | Slow | Easy | Most thorough reset |
| Emulator Wipe | Very Slow | Easy | Complete emulator reset |
