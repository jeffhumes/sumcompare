# Building Native Executables

SumCompare can be built as a native executable or installer package, eliminating the need for users to have Java installed.

## Option 1: jpackage - Native Installer (Recommended)

Creates platform-specific installers with bundled Java runtime.

### Requirements

- JDK 21 (includes jpackage tool)
- Platform-specific tools:
  - **Linux**: `dpkg-dev`, `fakeroot` (for .deb packages)
  - **macOS**: Xcode command line tools
  - **Windows**: WiX Toolset 3.x (for .msi installers)

### Build Instructions

```bash
# Run the build script
./build-native.sh
```

This creates:

- **Linux**: `target/dist/sumcompare_0.0.1_amd64.deb`
- **macOS**: `target/dist/SumCompare-0.0.1.dmg`
- **Windows**: `target/dist/SumCompare-0.0.1.msi`

### Install

**Linux:**

```bash
sudo dpkg -i target/dist/sumcompare_0.0.1_amd64.deb
```

**macOS:**
Open the DMG and drag SumCompare to Applications folder.

**Windows:**
Run the MSI installer.

### Features

✓ No Java installation required (bundles custom JRE)  
✓ Desktop shortcuts and menu entries  
✓ Standard installation/uninstallation  
✓ Professional installer experience

---

## Option 2: GraalVM Native Image - Standalone Binary

Compiles Java to native machine code for maximum performance.

### Requirements

- [GraalVM](https://www.graalvm.org/downloads/) 21
- Native Image component: `gu install native-image`
- Platform-specific build tools (gcc, etc.)

### Build Instructions

```bash
# Run the GraalVM build script
./build-graalvm.sh
```

This creates: `sumcompare-native` (standalone executable)

### Run

```bash
./sumcompare-native -s /source -t /target -z XXHASH64
```

### Features

✓ Instant startup (no JVM warmup)  
✓ Lower memory footprint  
✓ Single executable file  
✓ No Java runtime needed

### Limitations

⚠ **JavaFX GUI may not work** - GraalVM has limited JavaFX support  
⚠ Reflection requires configuration files  
⚠ Longer build times (5-10 minutes)

**Best for**: CLI-only usage where performance is critical

---

## Option 3: Executable JAR (Current)

Standard cross-platform JAR file.

### Build

```bash
mvn clean package
```

Creates: `target/sumcompare.jar`

### Run

```bash
# GUI
java -jar target/sumcompare.jar

# CLI
java -jar target/sumcompare.jar -s /source -t /target -z XXHASH64
```

### Features

✓ Cross-platform (works anywhere Java 21+ is installed)  
✓ Easiest to build and distribute  
✓ Full feature support (GUI + CLI)

### Requirements

- Java 21+ must be installed on user's system

---

## Comparison

| Method       | Size     | Startup | User Needs | GUI Support | Best For               |
| ------------ | -------- | ------- | ---------- | ----------- | ---------------------- |
| **jpackage** | ~50-80MB | Fast    | Nothing    | ✓ Full      | End users, Desktop app |
| **GraalVM**  | ~20-40MB | Instant | Nothing    | ⚠ Limited   | CLI, Performance       |
| **JAR**      | ~15MB    | Medium  | Java 21+   | ✓ Full      | Developers, Testing    |

---

## Recommendations

**For distribution to end users:**  
→ Use `./build-native.sh` (jpackage)

**For command-line power users:**  
→ Use `./build-graalvm.sh` (if it works) or JAR

**For development/testing:**  
→ Use JAR (`mvn package`)

---

## Troubleshooting

### jpackage: "jpackage: command not found"

- Ensure JDK 21 is in your PATH
- jpackage is included in JDK 14+

### GraalVM: Build fails with reflection errors

- JavaFX requires extensive reflection configuration
- Consider using jpackage instead for GUI apps
- For CLI-only, create reflection config files

### Linux: .deb packaging fails

```bash
sudo apt-get install dpkg-dev fakeroot
```

### Windows: .msi creation fails

- Install [WiX Toolset 3.x](https://wixtoolset.org/releases/)
- Add WiX to PATH

### macOS: Code signing issues

```bash
# For testing, allow unsigned apps
sudo spctl --master-disable
```
