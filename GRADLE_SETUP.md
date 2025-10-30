# Gradle Setup Instructions

## Current Status

✅ Gradle wrapper configuration files created
⚠️ Gradle wrapper JAR needs to be downloaded

## Quick Setup

Since the Gradle wrapper JAR file is too large to create directly, you have two options:

### Option 1: Download Gradle Wrapper JAR (Recommended)

1. Download the Gradle wrapper JAR from:
   https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar

2. Save it to:
   ```
   c:\Users\koenl\Documents\Minecolonies idea\minecoloniesllm\gradle\wrapper\gradle-wrapper.jar
   ```

3. Then run:
   ```powershell
   .\gradlew.bat build
   ```

### Option 2: Install Gradle Globally (Alternative)

1. **Using Chocolatey** (if you have it):
   ```powershell
   choco install gradle
   ```

2. **Manual Installation**:
   - Download from: https://gradle.org/releases/
   - Extract to `C:\Gradle`
   - Add `C:\Gradle\gradle-8.5\bin` to your PATH
   - Run: `gradle wrapper`
   - Then: `.\gradlew.bat build`

### Option 3: Use an Existing Minecraft Mod Project

If you have another NeoForge/Forge mod project:

1. Copy the `gradle` folder from that project
2. Copy the `gradlew.bat` file (already done)
3. Run: `.\gradlew.bat build`

## After Setup

Once Gradle is working, run:

```powershell
# Build the project
.\gradlew.bat build

# Generate IntelliJ IDEA files
.\gradlew.bat genIntellijRuns

# Run Minecraft client
.\gradlew.bat runClient
```

## Troubleshooting

### "Java not found"
- Install JDK 21 from: https://adoptium.net/
- Set JAVA_HOME environment variable

### "Permission denied"
- Run PowerShell as Administrator
- Or: `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy Bypass`

### "Cannot download Gradle"
- Check internet connection
- Check firewall settings
- Try using a VPN if downloads are blocked

## Verification

After setup, verify Gradle works:

```powershell
.\gradlew.bat --version
```

Should show:
```
Gradle 8.5
JVM: 21.x.x
OS: Windows
```

## Summary for Quick Start

**EASIEST PATH FOR YOU:**

1. Download this file:
   https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar

2. Put it in:
   `gradle\wrapper\gradle-wrapper.jar`

3. Run:
   ```powershell
   .\gradlew.bat build
   ```

That's it! The wrapper will handle everything else.
