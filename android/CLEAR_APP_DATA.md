# 🗑️ Clear App Data & Reseed Database

## Issue
You're seeing only 1 routine for Emma and none for Noah.

**Likely Cause:** Old/incomplete data from previous test runs.

---

## ✅ Solution: Clear App Data

### Method 1: In Emulator/Device (EASIEST)

1. **Long press** the app icon
2. **App info** (or drag to "App info")
3. **Storage** (or "Storage & cache")
4. **Clear storage** (or "Clear data")
5. **Confirm** "Delete app data"

### Method 2: Via Android Studio

1. In Android Studio, open **Device File Explorer** (bottom right)
2. Navigate to: `/data/data/com.routinechart/databases/`
3. **Right-click** on `routine_chart.db`
4. **Delete**
5. Do the same for any `.db-shm` and `.db-wal` files

### Method 3: Uninstall & Reinstall

1. **Uninstall** the app from emulator/device
2. **Run** from Android Studio again (will reinstall fresh)

---

## 🚀 After Clearing Data

**Run the app again:**

1. It will show "Loading..."
2. Seeds fresh database with:
   - ✅ Emma 🌟 with 2 routines (Morning ☀️ + Bedtime 🌙)
   - ✅ Noah 🚀 with 2 routines (Morning ☀️ + Bedtime 🌙)
3. Both children should have **10 total steps** (5 per routine)

---

## 🔍 Verify It's Working

After clearing data and re-running:

### For Emma 🌟:
- [ ] Shows 2 routines: "Morning Routine ☀️" and "Bedtime Routine 🌙"
- [ ] Each routine has 5 steps
- [ ] Can tap steps to complete (green checkmark)
- [ ] Can tap again to undo (gray circle)

### For Noah 🚀:
- [ ] Tap Noah's chip at the top
- [ ] Shows 2 routines: "Morning Routine ☀️" and "Bedtime Routine 🌙"
- [ ] Each routine has 5 steps
- [ ] Can tap steps to complete/undo

---

## 📊 Expected Behavior

**Emma's Morning Routine:**
1. Wake up 🛏️
2. Brush teeth 🦷
3. Get dressed 👕
4. Eat breakfast 🍳
5. Pack backpack 🎒

**Emma's Bedtime Routine:**
1. Put on pajamas 👘
2. Brush teeth 🦷
3. Read a book 📚
4. Say goodnight 💤
5. Lights out 🌃

**Noah has the same routines!**

---

## 🐛 If Still Wrong After Clearing

If you still see incomplete data after clearing app data, check Logcat for errors:

```
Filter: "RoutineChart"
```

Look for:
- "Seed data created: 1 family, 2 children, 2 routines, 10 steps, 4 assignments"
- "Loading routines for childId=..."
- Any error messages

Then share the logs with me.

---

**Try clearing app data first - that should fix it!** 🎯

