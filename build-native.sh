#!/bin/bash
# Build native executable/installer with jpackage
# Requires JDK 21 with jpackage tool

set -e

echo "==================================================================="
echo "Building SumCompare Native Installer"
echo "==================================================================="

# Clean and build the JAR
echo "Step 1: Building JAR..."
mvn clean package

# Create custom JRE with jlink (reduces size)
echo "Step 2: Creating custom Java runtime..."
jlink \
  --add-modules java.base,java.desktop,java.logging,java.xml,java.naming,java.sql,jdk.unsupported,jdk.crypto.ec \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=2 \
  --output target/java-runtime

# Detect OS and create appropriate installer
echo "Step 3: Creating native installer..."

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Building Linux package..."
    jpackage \
      --type deb \
      --input target \
      --name SumCompare \
      --main-jar sumcompare.jar \
      --main-class org.bofus.sumcompare.gui.SumCompareGUI \
      --runtime-image target/java-runtime \
      --app-version 0.0.1 \
      --vendor "org.bofus" \
      --description "Intelligent File Deduplication Tool" \
      --linux-shortcut \
      --linux-menu-group "Utility" \
      --linux-app-category "Utility" \
      --dest target/dist
    
    echo ""
    echo "✓ Linux package created: target/dist/sumcompare_0.0.1_amd64.deb"
    echo ""
    echo "To install:"
    echo "  sudo dpkg -i target/dist/sumcompare_0.0.1_amd64.deb"

elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Building macOS package..."
    jpackage \
      --type dmg \
      --input target \
      --name SumCompare \
      --main-jar sumcompare.jar \
      --main-class org.bofus.sumcompare.gui.SumCompareGUI \
      --runtime-image target/java-runtime \
      --app-version 0.0.1 \
      --vendor "org.bofus" \
      --description "Intelligent File Deduplication Tool" \
      --mac-package-name SumCompare \
      --dest target/dist
    
    echo ""
    echo "✓ macOS package created: target/dist/SumCompare-0.0.1.dmg"
    echo ""
    echo "To install: Open the DMG and drag SumCompare to Applications"

elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    echo "Building Windows package..."
    jpackage \
      --type msi \
      --input target \
      --name SumCompare \
      --main-jar sumcompare.jar \
      --main-class org.bofus.sumcompare.gui.SumCompareGUI \
      --runtime-image target/java-runtime \
      --app-version 0.0.1 \
      --vendor "org.bofus" \
      --description "Intelligent File Deduplication Tool" \
      --win-dir-chooser \
      --win-shortcut \
      --win-menu \
      --win-menu-group SumCompare \
      --dest target/dist
    
    echo ""
    echo "✓ Windows installer created: target/dist/SumCompare-0.0.1.msi"
    echo ""
    echo "To install: Run the MSI installer"
fi

echo ""
echo "==================================================================="
echo "Build Complete!"
echo "==================================================================="
echo ""
echo "The installer includes:"
echo "  - SumCompare application"
echo "  - Custom Java runtime (no Java installation required)"
echo "  - Desktop shortcuts and menu entries"
echo ""
