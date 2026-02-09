# JCStaff Keystore Files

This directory contains the signing key files for the project to ensure consistent app signatures across different machines, avoiding the need to uninstall and reinstall.

## File Description

### debug.keystore
- **Purpose**: Debug build signing
- **Keystore Password**: `android`
- **Key Alias**: `androiddebugkey`
- **Key Password**: `android`
- **Validity**: 10000 days

### release.keystore
- **Purpose**: Release build signing
- **Keystore Password**: `jcstaff`
- **Key Alias**: `jcstaff`
- **Key Password**: `jcstaff`
- **Validity**: 10000 days

## Security Notice

⚠️ **These are keystore files for open source projects only**

These keystore files are public and **intended for open source development and testing purposes only**. Do not use these keys for production environments or official app releases.

## Usage

These keystores are already configured in `app/build.gradle.kts`, no additional setup needed:

```kotlin
signingConfigs {
    getByName("debug") {
        storeFile = file("../keystore/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }
    create("release") {
        storeFile = file("../keystore/release.keystore")
        storePassword = "jcstaff"
        keyAlias = "jcstaff"
        keyPassword = "jcstaff"
    }
}
```

## Regenerating Keystores

If you need to regenerate the keystores, use the following commands:

```bash
# Debug keystore
keytool -genkeypair -v -keystore debug.keystore -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"

# Release keystore
keytool -genkeypair -v -keystore release.keystore -alias jcstaff \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass jcstaff -keypass jcstaff \
  -dname "CN=JCStaff,O=OpenSource,C=US"
```

## Viewing Keystore Information

```bash
# View debug keystore
keytool -list -v -keystore debug.keystore -storepass android

# View release keystore
keytool -list -v -keystore release.keystore -storepass jcstaff
```
