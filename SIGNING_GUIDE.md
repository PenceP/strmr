# App Signing Guide for Updates

## Issue: Signature Mismatch Error

When you see the error "Existing package com.strmr.ai signatures do not match newer version", it means the APK you're trying to install was signed with a different key than the currently installed version.

## Solution

### For Development/Testing

1. **Quick Fix**: Uninstall the current app before installing the update
   ```bash
   adb uninstall com.strmr.ai
   ```

2. **Then install the new version from GitHub releases**

### For Production Releases

Always use the same keystore for all releases:

1. **Create a release keystore** (if you haven't already):
   ```bash
   keytool -genkey -v -keystore strmr-release.keystore -alias strmr -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure your build.gradle** to use this keystore for release builds:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('path/to/strmr-release.keystore')
               storePassword 'your-store-password'
               keyAlias 'strmr'
               keyPassword 'your-key-password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               // ... other release config
           }
       }
   }
   ```

3. **GitHub Actions**: If using CI/CD, store your keystore as a base64 encoded secret and decode it during the build process

## Important Notes

- **Keep your keystore safe**: Losing it means you can't update your app
- **Never commit your keystore** to version control
- **Use the same keystore** for all releases (GitHub, Play Store, etc.)
- **For testing**: You can use debug builds which are automatically signed with a debug key

## Verifying APK Signatures

To check which key an APK is signed with:
```bash
# For installed app
adb shell dumpsys package com.strmr.ai | grep signatures

# For APK file
keytool -printcert -jarfile your-app.apk
```