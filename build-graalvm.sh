#!/bin/bash
# Build native executable with GraalVM Native Image
# Requires GraalVM with native-image installed

set -e

echo "==================================================================="
echo "Building SumCompare GraalVM Native Image"
echo "==================================================================="

# Check if native-image is available
if ! command -v native-image &> /dev/null; then
    echo "ERROR: native-image not found!"
    echo ""
    echo "Please install GraalVM and native-image:"
    echo "  1. Download GraalVM: https://www.graalvm.org/downloads/"
    echo "  2. Install native-image: gu install native-image"
    echo ""
    exit 1
fi

# Clean and build the JAR
echo "Step 1: Building JAR..."
mvn clean package

# Create native image
echo "Step 2: Building native executable (this may take several minutes)..."
native-image \
  -jar target/sumcompare.jar \
  --no-fallback \
  -H:+ReportExceptionStackTraces \
  -H:IncludeResources=".*\\.fxml|.*\\.css|.*\\.xml|.*\\.properties" \
  --initialize-at-build-time=org.slf4j,ch.qos.logback \
  --initialize-at-run-time=javafx \
  -H:Name=sumcompare-native \
  -H:+AddAllCharsets \
  --enable-url-protocols=http,https \
  --verbose

if [ $? -eq 0 ]; then
    echo ""
    echo "==================================================================="
    echo "✓ Native executable created: sumcompare-native"
    echo "==================================================================="
    echo ""
    echo "Size comparison:"
    ls -lh target/sumcompare.jar sumcompare-native 2>/dev/null || true
    echo ""
    echo "To run:"
    echo "  ./sumcompare-native -h"
    echo "  ./sumcompare-native -s /source -t /target -z XXHASH64"
    echo ""
    echo "Benefits:"
    echo "  - Instant startup (no JVM warmup)"
    echo "  - Lower memory usage"
    echo "  - No Java runtime required"
    echo ""
else
    echo ""
    echo "ERROR: Native image build failed!"
    echo "This is common with JavaFX and reflection-heavy code."
    echo "You may need to add reflection configuration files."
    exit 1
fi
