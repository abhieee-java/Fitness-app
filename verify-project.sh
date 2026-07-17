#!/bin/bash
# Self-healing verification script for local and CI environments.
set -e

echo "==============================================="
echo "🏋️‍♂️ Running The Judge Applet Self-Healing Check 🏋️‍♂️"
echo "==============================================="

echo "➡️ Step 1: Cleaning cache & sync checks..."
gradle clean --console=plain

echo "➡️ Step 2: Running Unit and Robolectric Tests..."
if gradle :app:testDebugUnitTest --console=plain; then
    echo "✅ Unit and Robolectric tests PASSED!"
else
    echo "❌ Tests FAILED!"
    exit 1
fi

echo "➡️ Step 3: Compiling and Building Debug APK..."
if gradle assembleDebug --console=plain; then
    echo "✅ Debug APK built successfully!"
    echo "📍 Debug APK is located at: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Build FAILED!"
    exit 1
fi

echo "==============================================="
echo "🎉 Validation SUCCESSFUL! All checks passed!"
echo "==============================================="
