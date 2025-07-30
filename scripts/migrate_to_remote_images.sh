#!/bin/bash

# Migration script to move drawable images to remote GitHub loading
# This will save ~75MB in APK size

echo "🚀 Starting migration to remote image loading..."

DRAWABLE_DIR="app/src/main/res/drawable"
BACKUP_DIR="drawable_backup_$(date +%Y%m%d_%H%M%S)"
GITHUB_BASE_URL="https://raw.githubusercontent.com/PenceP/strmr/refs/heads/main/app/src/main/res/drawable/"

# Create backup directory
echo "📦 Creating backup of drawable resources..."
mkdir -p "$BACKUP_DIR"
cp -r "$DRAWABLE_DIR" "$BACKUP_DIR/"
echo "✅ Backup created at: $BACKUP_DIR"

# List files that will be moved
echo ""
echo "📊 Analyzing drawable resources to migrate:"
echo "============================================="

collection_files=0
director_files=0
network_files=0
other_files=0
total_size=0

for file in "$DRAWABLE_DIR"/*.png; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        size=$(ls -lh "$file" | awk '{print $5}')
        size_bytes=$(wc -c < "$file")
        total_size=$((total_size + size_bytes))
        
        if [[ "$filename" == collection_* ]]; then
            echo "🎬 Collection: $filename ($size)"
            collection_files=$((collection_files + 1))
        elif [[ "$filename" == director_* ]]; then
            echo "🎭 Director: $filename ($size)"
            director_files=$((director_files + 1))
        elif [[ "$filename" == network_* ]]; then
            echo "📺 Network: $filename ($size)"
            network_files=$((network_files + 1))
        else
            echo "📄 Other: $filename ($size)"
            other_files=$((other_files + 1))
        fi
    fi
done

total_size_mb=$((total_size / 1024 / 1024))
echo ""
echo "📈 Migration Summary:"
echo "===================="
echo "Collection images: $collection_files"
echo "Director images: $director_files"
echo "Network images: $network_files"
echo "Other images: $other_files"
echo "Total size to save: ${total_size_mb}MB"

echo ""
echo "🔧 Verification - Testing GitHub URLs:"
echo "======================================"

# Test a few URLs to make sure they're accessible
test_files=("collection_007.png" "director_nolan.png" "network_netflix.jpg")
for test_file in "${test_files[@]}"; do
    test_url="${GITHUB_BASE_URL}${test_file}"
    echo -n "Testing $test_file... "
    
    if curl -s --head "$test_url" | head -n 1 | grep -q "200 OK"; then
        echo "✅ Available"
    else
        echo "❌ Not found - URL: $test_url"
    fi
done

echo ""
read -p "🤔 Do you want to proceed with removing these files from the APK? (y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "🗑️ Removing drawable files from APK..."
    
    # Remove collection images
    rm -f "$DRAWABLE_DIR"/collection_*.png
    echo "✅ Removed collection images"
    
    # Remove director images  
    rm -f "$DRAWABLE_DIR"/director_*.png
    echo "✅ Removed director images"
    
    # Remove network images
    rm -f "$DRAWABLE_DIR"/network_*.avif
    rm -f "$DRAWABLE_DIR"/network_*.jpg
    echo "✅ Removed network images"
    
    # Keep essential images (logos, icons, etc.)
    echo "⚠️  Keeping essential images (logos, icons, breaking_bad_logo.png, etc.)"
    
    echo ""
    echo "🎉 Migration completed successfully!"
    echo "📊 Estimated APK size reduction: ${total_size_mb}MB"
    echo "💾 Files backed up to: $BACKUP_DIR"
    echo ""
    echo "🔍 Next steps:"
    echo "1. Build your APK: ./gradlew assembleRelease"
    echo "2. Test the remote loading on first app launch"
    echo "3. Verify all images load correctly"
    echo "4. Run: ./scripts/analyze_apk_size.sh"
    echo ""
    echo "⚠️  Note: Images will now load from GitHub on first launch"
    echo "   and be cached locally for 7 days."
    
else
    echo ""
    echo "❌ Migration cancelled. No files were removed."
    echo "💾 Backup is still available at: $BACKUP_DIR"
fi

echo ""
echo "🏁 Script completed."