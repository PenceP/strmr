# APK Size Optimization Plan: 300MB → 70-120MB

## Current Analysis
- **Current APK Size**: 300MB+
- **Target Size**: 70-120MB  
- **Required Reduction**: 180-230MB (60-75% reduction)

## Major Size Contributors & Solutions

### 1. 🔴 CRITICAL: LibVLC Library (~80-120MB)
**Problem**: `org.videolan.android:libvlc-all:4.0.0-eap15` includes native libs for all architectures
**Impact**: 25-40% of APK size

**Solutions**:
```kotlin
// Option A: Target Android TV only (arm64-v8a)
android {
    splits {
        abi {
            enable true
            reset()
            include "arm64-v8a"  // Primary Android TV architecture
            include "armeabi-v7a" // Legacy Android TV support
            universalApk false
        }
    }
}
```

```kotlin
// Option B: Remove LibVLC entirely if ExoPlayer sufficient
// Remove: implementation("org.videolan.android:libvlc-all:4.0.0-eap15")
```

```kotlin
// Option C: Use LibVLC minimal build
implementation("org.videolan.android:libvlc-android:4.0.0-eap15") // ~40MB smaller
```

### 2. 🟡 HIGH: ExoPlayer Modules (~20-30MB)
**Problem**: Multiple redundant ExoPlayer dependencies

**Current**:
```kotlin
implementation("androidx.media3:media3-exoplayer:1.6.1")
implementation("androidx.media3:media3-exoplayer-dash:1.6.1") 
implementation("androidx.media3:media3-exoplayer-hls:1.6.1")
implementation("androidx.media3:media3-ui:1.6.1")
implementation("androidx.media3:media3-datasource-okhttp:1.6.1")
implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.6.1") // Remove if unused
```

**Optimized**:
```kotlin
// Use BOM for version alignment
implementation(platform("androidx.media3:media3-bom:1.6.1"))
implementation("androidx.media3:media3-exoplayer")
implementation("androidx.media3:media3-exoplayer-dash")
implementation("androidx.media3:media3-exoplayer-hls") 
implementation("androidx.media3:media3-ui")
implementation("androidx.media3:media3-datasource-okhttp")
// Remove SmoothStreaming unless needed
```

### 3. 🟡 MEDIUM: Drawable Images (75MB)
**Current**: 51 PNG files, ~1.5MB each in `/res/drawable/`

**Solutions**:
```kotlin
// A. Move to remote loading (as you mentioned)
// B. Convert to WebP (70% size reduction)
// C. Use vector drawables for simple images
// D. Implement drawable loading fallbacks
```

### 4. 🟠 BUILD CONFIGURATION OPTIMIZATIONS

**Enhanced build.gradle.kts**:
```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true  // Enable resource shrinking
            isDebuggable = false
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Enable additional optimizations
            postprocessing {
                removeUnusedCode = true
                removeUnusedResources = true
                obfuscate = true
                isOptimizeCode = true
            }
        }
    }
    
    // Target specific densities for Android TV
    defaultConfig {
        resourceConfigurations += listOf("en", "xhdpi", "xxhdpi")
    }
    
    // Exclude unnecessary architectures initially
    packagingOptions {
        jniLibs {
            excludes += listOf(
                "**/x86/**",
                "**/x86_64/**"  // Exclude if not targeting x86 Android TV
            )
        }
    }
    
    // Bundle configuration
    bundle {
        language {
            enableSplit = false  // Keep languages in base APK for sideloading
        }
        density {
            enableSplit = false  // Keep all densities for different TV resolutions
        }
        abi {
            enableSplit = true   // Split by architecture
        }
    }
}
```

### 5. 🟢 GITHUB ACTIONS OPTIMIZATION

**Enhanced CI/CD Pipeline**:
```yaml
- name: Build optimized release APK
  run: |
    ./gradlew assembleRelease --parallel --configure-on-demand
    
- name: Analyze APK size
  run: |
    ./gradlew :app:analyzeReleaseBundle
    ls -la app/build/outputs/apk/release/
    
- name: Upload APK analysis
  uses: actions/upload-artifact@v4
  with:
    name: apk-analysis
    path: app/build/reports/
```

## Size Reduction Estimates

| Optimization | Current Size | Optimized Size | Savings |
|--------------|-------------|----------------|---------|
| LibVLC (ABI split) | ~100MB | ~25MB | 75MB |
| LibVLC (remove) | ~100MB | 0MB | 100MB |
| Drawable → Remote | 75MB | 5MB | 70MB |
| ExoPlayer cleanup | ~25MB | ~15MB | 10MB |
| Resource shrinking | ~20MB | ~5MB | 15MB |
| Unused dependencies | ~15MB | ~5MB | 10MB |
| **TOTAL POTENTIAL** | **335MB** | **55-85MB** | **180-280MB** |

## Implementation Priority

### Phase 1: Critical (Immediate 70% reduction)
1. ✅ Enable ABI splits for LibVLC
2. ✅ Move drawable images to remote loading
3. ✅ Enable resource shrinking

### Phase 2: High Impact (Additional 10-15% reduction)  
1. ✅ Clean up ExoPlayer dependencies
2. ✅ Remove unused libraries
3. ✅ Optimize ProGuard rules

### Phase 3: Fine-tuning (Final 5-10% reduction)
1. ✅ Convert remaining images to WebP
2. ✅ Remove unused resources
3. ✅ Language/density optimization

## Expected Final Size: 65-95MB ✅

This plan should easily get you from 300MB to your target of 70-120MB, with potential to go even lower.