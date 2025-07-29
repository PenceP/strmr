#!/bin/bash

# APK Size Analysis Script
echo "🔍 Analyzing APK sizes..."

APK_DIR="app/build/outputs/apk/release"

if [ ! -d "$APK_DIR" ]; then
    echo "❌ APK directory not found. Run ./gradlew assembleRelease first."
    exit 1
fi

echo ""
echo "📱 APK Files Generated:"
echo "========================"

# List all APK files with sizes
find "$APK_DIR" -name "*.apk" -exec ls -lh {} \; | while read -r line; do
    size=$(echo "$line" | awk '{print $5}')
    file=$(echo "$line" | awk '{print $9}')
    filename=$(basename "$file")
    
    echo "📦 $filename: $size"
    
    # Color coding based on size
    size_mb=$(echo "$size" | sed 's/M//' | sed 's/K//' | cut -d'.' -f1)
    if [[ "$size" == *"M"* ]]; then
        if [ "$size_mb" -lt 80 ]; then
            echo "   ✅ Excellent size!"
        elif [ "$size_mb" -lt 120 ]; then
            echo "   🟡 Good size"
        elif [ "$size_mb" -lt 200 ]; then
            echo "   🟠 Could be smaller"
        else
            echo "   🔴 Too large - needs optimization"
        fi
    fi
done

echo ""
echo "🔧 Size Optimization Tips:"
echo "=========================="
echo "1. 🎯 Remove LibVLC if not needed (saves ~80-120MB)"
echo "2. 📱 Move drawable images to remote loading (saves ~75MB)"  
echo "3. ⚙️  Enable more aggressive ProGuard rules"
echo "4. 🗜️  Convert remaining images to WebP format"
echo "5. 🏗️  Remove unused dependencies"

echo ""
echo "🎯 Target: 70-120MB for optimal Android TV distribution"