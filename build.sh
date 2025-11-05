#!/bin/bash
# Quick build script selector

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║             SumCompare - Build Script Selector               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "Choose your build option:"
echo ""
echo "  1) Standard JAR (requires Java 21 on user's system)"
echo "     → Fast build, cross-platform, full features"
echo "     → Command: mvn clean package"
echo ""
echo "  2) Native Installer (jpackage - RECOMMENDED)"
echo "     → .deb/.dmg/.msi with bundled Java"
echo "     → No Java needed for end users"
echo "     → Command: ./build-native.sh"
echo ""
echo "  3) GraalVM Native Image (experimental)"
echo "     → Single binary, instant startup"
echo "     → JavaFX GUI may not work"
echo "     → Command: ./build-graalvm.sh"
echo ""
echo "  4) Exit"
echo ""
read -p "Enter choice [1-4]: " choice

case $choice in
    1)
        echo ""
        echo "Building standard JAR..."
        mvn clean package
        ;;
    2)
        echo ""
        if [ ! -f "build-native.sh" ]; then
            echo "ERROR: build-native.sh not found!"
            exit 1
        fi
        ./build-native.sh
        ;;
    3)
        echo ""
        if [ ! -f "build-graalvm.sh" ]; then
            echo "ERROR: build-graalvm.sh not found!"
            exit 1
        fi
        ./build-graalvm.sh
        ;;
    4)
        echo "Goodbye!"
        exit 0
        ;;
    *)
        echo "Invalid choice!"
        exit 1
        ;;
esac
