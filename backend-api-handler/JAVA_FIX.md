# Java Version Issue Fix

## Problem
You're using Java 25, but this project targets Java 17. Lombok has compatibility issues with Java 25.

## ✅ EASIEST Solution: Use JAVA_HOME to specify Java 17

### Option 1: Temporary (for current session)

**PowerShell:**
```powershell
# Set Java 17 path (adjust path to where YOUR Java 17 is installed)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Verify
java -version

# Now run your app
mvn clean spring-boot:run
```

### Option 2: Download Java 17 (if you don't have it)

1. **Download Java 17 (LTS):**
   - https://www.oracle.com/java/technologies/downloads/#java17
   - Or use OpenJDK: https://adoptium.net/temurin/releases/?version=17

2. **Install it**

3. **Set JAVA_HOME** (as shown in Option 1)

### Option 3: Use Maven Toolchains (Advanced)

This tells Maven to use Java 17 even if your system Java is 25.

**But the easiest is just to use Java 17!**

## Alternative: Use Pre-compiled JAR

If the app was previously compiled successfully, you can just run the JAR directly:

```powershell
java -jar target\java-maven-springboot-0.0.1-SNAPSHOT.jar
```

---

## Quick Check for Java 17 Installation

```powershell
# Check all Java installations
where.exe java

# Common locations:
# C:\Program Files\Java\jdk-17
# C:\Program Files\Eclipse Adoptium\jdk-17
# C:\Program Files\OpenJDK\jdk-17
```

If you find Java 17, just set JAVA_HOME to that path and you're good to go!

