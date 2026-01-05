#!/bin/bash
# Clean Gradle build files and caches

echo "🧹 Cleaning Gradle build files..."

cd "$(dirname "$0")"

# Remove build directories
rm -rf app/build
rm -rf build
rm -rf .gradle

# Remove Android Studio files (optional)
rm -rf .idea
rm -rf *.iml
rm -rf app/*.iml

echo "✅ Clean complete!"
echo ""
echo "Now open Android Studio and:"
echo "1. File → Open → Select 'android' folder"
echo "2. File → Invalidate Caches → Invalidate and Restart"
echo "3. Wait for Gradle sync"
echo "4. Run the app"

