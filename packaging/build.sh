#!/bin/bash
# ============================================================
# EMMS Lite - Build Script (for development on macOS/Linux)
# ============================================================
# Builds JARs and creates a bundled JRE via jlink.
# The resulting build/ directory is self-contained.
#
# For production Windows installer, use build-installer.bat.
# ============================================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$PROJECT_ROOT/build"
APP_VERSION="1.0.0"

echo "============================================================"
echo " EMMS Lite - Development Build"
echo "============================================================"

# --- Step 1: Build backend ---
echo ""
echo "[1/4] Building backend..."
cd "$PROJECT_ROOT/backend"
mvn clean package -DskipTests -q
echo "Backend build successful."

# --- Step 2: Build frontend ---
echo ""
echo "[2/4] Building frontend..."
cd "$PROJECT_ROOT/frontend"
mvn clean package -DskipTests -q
echo "Frontend build successful."

# --- Step 3: Create bundled JRE with jlink ---
echo ""
echo "[3/4] Creating bundled JRE (Java 21 runtime)..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

if ! command -v jlink &> /dev/null; then
    echo "WARNING: jlink not found. Skipping JRE bundling."
    echo "         Install JDK 21+ for jlink support."
else
    JMODS_DIR="$JAVA_HOME/jmods"
    if [ ! -d "$JMODS_DIR" ]; then
        JMODS_DIR="$(dirname "$(dirname "$(readlink -f "$(which java)")")"))/jmods"
    fi
    jlink \
        --module-path "$JMODS_DIR" \
        --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.management \
        --output "$BUILD_DIR/runtime" \
        --strip-debug \
        --compress zip-6 \
        --no-header-files \
        --no-man-pages
    echo "Bundled JRE created at: $BUILD_DIR/runtime"
fi

# --- Step 4: Stage output ---
echo ""
echo "[4/4] Staging build output..."
mkdir -p "$BUILD_DIR/app/config" "$BUILD_DIR/app/data" "$BUILD_DIR/app/logs"

cp "$PROJECT_ROOT"/backend/target/emms-backend-*.jar "$BUILD_DIR/app/emms-backend.jar"
cp "$PROJECT_ROOT"/frontend/target/emms-frontend-*.jar "$BUILD_DIR/app/emms-frontend.jar"
cp "$PROJECT_ROOT/backend/src/main/resources/application.properties" "$BUILD_DIR/app/config/"
echo "$APP_VERSION" > "$BUILD_DIR/app/version.txt"

# Copy bundled JRE into app directory
if [ -d "$BUILD_DIR/runtime" ]; then
    cp -r "$BUILD_DIR/runtime" "$BUILD_DIR/app/runtime"
fi

echo ""
echo "============================================================"
echo " Build complete! Output in: $BUILD_DIR/app/"
echo ""
if [ -d "$BUILD_DIR/app/runtime" ]; then
    JAVA_CMD="$BUILD_DIR/app/runtime/bin/java"
    echo " Bundled JRE available. To run locally:"
else
    JAVA_CMD="java"
    echo " No bundled JRE (jlink not available). Using system Java."
    echo " To run locally:"
fi
echo "   1. Start backend:"
echo "      $JAVA_CMD -jar $BUILD_DIR/app/emms-backend.jar \\"
echo "        --spring.config.location=$BUILD_DIR/app/config/application.properties"
echo ""
echo "   2. Start frontend:"
echo "      $JAVA_CMD -jar $BUILD_DIR/app/emms-frontend.jar"
echo "============================================================"
