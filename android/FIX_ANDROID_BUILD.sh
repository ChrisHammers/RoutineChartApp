#!/bin/bash

# Fix Android Build - Complete Clean and Rebuild
# This script fixes the Hilt NoClassDefFoundError

set -e

echo "🧹 Step 1: Uninstalling old apps..."
adb uninstall com.routinechart 2>/dev/null || echo "  com.routinechart not installed"
adb uninstall com.HammersTech.RoutineChart 2>/dev/null || echo "  com.HammersTech.RoutineChart not installed"

echo ""
echo "🗑️  Step 2: Cleaning build..."
./gradlew clean

echo ""
echo "🗑️  Step 3: Removing build directories..."
rm -rf app/build
rm -rf build
rm -rf .gradle

echo ""
echo "🔨 Step 4: Building debug APK..."
./gradlew assembleDebug

echo ""
echo "📱 Step 5: Installing..."
./gradlew installDebug

echo ""
echo "✅ Done! The app should now launch without crashing."
echo ""
echo "If it still crashes, try:"
echo "  1. Restart Android Studio"
echo "  2. File → Invalidate Caches → Invalidate and Restart"
echo "  3. Run this script again"

