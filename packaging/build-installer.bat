@echo off
REM ============================================================
REM EMMS Lite - Windows Installer Build Script
REM ============================================================
REM Prerequisites:
REM   - JDK 21+ with jpackage and jlink
REM   - Maven 3.9+
REM   - Inno Setup 6 (optional, for EXE installer)
REM
REM The output is a self-contained installer that ships a
REM bundled JRE (via jlink). End users do NOT need Java.
REM ============================================================

setlocal enabledelayedexpansion

echo ============================================================
echo  EMMS Lite - Build and Package
echo ============================================================

set PROJECT_ROOT=%~dp0..
set BUILD_DIR=%PROJECT_ROOT%\build
set DIST_DIR=%PROJECT_ROOT%\dist
set APP_VERSION=1.0.0
set APP_NAME=EMMS Lite
set APP_VENDOR=Aircraft Operations
set INSTALL_DIR=AircraftApp

REM --- Verify JDK 21 ---
echo.
echo Checking JDK...
java -version 2>&1 | findstr /i "21" >nul
if errorlevel 1 (
    echo WARNING: JDK 21 not detected. Build may fail.
)

where jlink >nul 2>&1
if errorlevel 1 (
    echo ERROR: jlink not found on PATH. Ensure JDK 21+ is installed for building.
    exit /b 1
)

REM --- Step 1: Build backend ---
echo.
echo [1/6] Building backend...
cd /d "%PROJECT_ROOT%\backend"
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo ERROR: Backend build failed.
    exit /b 1
)
echo Backend build successful.

REM --- Step 2: Build frontend ---
echo.
echo [2/6] Building frontend...
cd /d "%PROJECT_ROOT%\frontend"
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo ERROR: Frontend build failed.
    exit /b 1
)
echo Frontend build successful.

REM --- Step 3: Create bundled JRE with jlink ---
echo.
echo [3/6] Creating bundled JRE (Java 21 runtime)...
if exist "%BUILD_DIR%\runtime" rmdir /s /q "%BUILD_DIR%\runtime"

REM Use java.se (all standard Java SE modules) plus extras needed by Spring Boot
jlink ^
    --module-path "%JAVA_HOME%\jmods" ^
    --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.management ^
    --output "%BUILD_DIR%\runtime" ^
    --strip-debug ^
    --compress zip-6 ^
    --no-header-files ^
    --no-man-pages

if errorlevel 1 (
    echo ERROR: jlink failed. Verify JAVA_HOME points to a valid JDK 21+.
    echo JAVA_HOME=%JAVA_HOME%
    exit /b 1
)
echo Bundled JRE created at: %BUILD_DIR%\runtime

REM --- Step 4: Prepare staging directory ---
echo.
echo [4/6] Preparing staging directory...
if exist "%BUILD_DIR%\app" rmdir /s /q "%BUILD_DIR%\app"
mkdir "%BUILD_DIR%\app"
mkdir "%BUILD_DIR%\app\config"
mkdir "%BUILD_DIR%\app\data"
mkdir "%BUILD_DIR%\app\logs"

REM Copy backend JAR
copy /y "%PROJECT_ROOT%\backend\target\emms-backend-*.jar" "%BUILD_DIR%\app\emms-backend.jar"

REM Copy frontend JAR (fat JAR with all dependencies)
copy /y "%PROJECT_ROOT%\frontend\target\emms-frontend-*.jar" "%BUILD_DIR%\app\emms-frontend.jar"

REM Copy config
copy /y "%PROJECT_ROOT%\backend\src\main\resources\application.properties" "%BUILD_DIR%\app\config\application.properties"

REM Copy launcher script
copy /y "%PROJECT_ROOT%\packaging\emms-launcher.bat" "%BUILD_DIR%\app\emms-launcher.bat"

REM Copy bundled JRE into staging
echo Copying bundled JRE into staging area...
xcopy /e /i /q /y "%BUILD_DIR%\runtime" "%BUILD_DIR%\app\runtime" >nul

REM Copy version file
echo %APP_VERSION% > "%BUILD_DIR%\app\version.txt"

REM --- Step 5: Create installer ---
echo.
echo [5/6] Creating Windows installer...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"

REM Try Inno Setup first (recommended for this architecture)
where iscc >nul 2>&1
if %errorlevel% equ 0 (
    echo Using Inno Setup...
    iscc "%PROJECT_ROOT%\packaging\emms-installer.iss"
    if errorlevel 1 (
        echo WARNING: Inno Setup failed. Trying jpackage fallback...
        goto :try_jpackage
    )
    goto :build_done
)

:try_jpackage
REM Fall back to jpackage (creates its own runtime, but we override with --runtime-image)
echo Using jpackage...
jpackage ^
    --type exe ^
    --name "%APP_NAME%" ^
    --app-version %APP_VERSION% ^
    --vendor "%APP_VENDOR%" ^
    --description "Aircraft Operations Management System" ^
    --dest "%DIST_DIR%" ^
    --input "%BUILD_DIR%\app" ^
    --main-jar emms-frontend.jar ^
    --main-class com.aircraft.emms.ui.EmmsLauncher ^
    --runtime-image "%BUILD_DIR%\runtime" ^
    --java-options "-Xmx512m" ^
    --install-dir "%INSTALL_DIR%" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --win-shortcut-prompt ^
    --resource-dir "%PROJECT_ROOT%\packaging\resources"

if errorlevel 1 (
    echo WARNING: jpackage exe creation failed. Trying msi...
    jpackage ^
        --type msi ^
        --name "%APP_NAME%" ^
        --app-version %APP_VERSION% ^
        --vendor "%APP_VENDOR%" ^
        --description "Aircraft Operations Management System" ^
        --dest "%DIST_DIR%" ^
        --input "%BUILD_DIR%\app" ^
        --main-jar emms-frontend.jar ^
        --main-class com.aircraft.emms.ui.EmmsLauncher ^
        --runtime-image "%BUILD_DIR%\runtime" ^
        --java-options "-Xmx512m" ^
        --install-dir "%INSTALL_DIR%" ^
        --win-dir-chooser ^
        --win-menu ^
        --win-shortcut
)

:build_done
echo.
echo [6/6] Build complete!
echo.
echo Installer created in: %DIST_DIR%
echo.
echo NOTE: The installer includes a bundled Java 21 runtime.
echo       End users do NOT need Java installed.
echo.
echo ============================================================
echo  Installation Layout:
echo    C:\%INSTALL_DIR%\
echo       bin\         - Application binaries
echo       lib\         - Libraries
echo       config\      - Configuration files
echo       data\        - Database (PRESERVED ON UPDATE)
echo       uploads\     - Uploaded XMLs (PRESERVED ON UPDATE)
echo       logs\        - Application logs
echo ============================================================

endlocal
