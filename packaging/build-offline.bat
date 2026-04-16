@echo off
REM ============================================================
REM EMMS Lite - Offline Build & Package Script for Windows
REM ============================================================
REM Prerequisites (install on the Windows build machine):
REM   1. JDK 21 (Adoptium Temurin recommended)
REM   2. Apache Maven 3.9+
REM   3. Inno Setup 6.x (for .exe installer)
REM   4. WiX Toolset 3.x (optional, only for .msi via jpackage)
REM
REM This script uses the pre-cached Maven repo in lib\maven-repo
REM so NO internet connection is required.
REM ============================================================

setlocal enabledelayedexpansion

set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"

echo.
echo =========================================
echo  EMMS Lite - Offline Build
echo =========================================
echo.

REM --- Step 0: Verify prerequisites ---
echo [Step 0] Checking prerequisites...
where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found. Install JDK 21 and add to PATH.
    exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%~v
echo   Java version: %JAVA_VER%

where mvn >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven not found. Install Maven 3.9+ and add to PATH.
    exit /b 1
)
echo   Maven found: OK

where jlink >nul 2>&1
if errorlevel 1 (
    echo ERROR: jlink not found. Ensure JDK 21 bin is on PATH.
    exit /b 1
)
echo   jlink found: OK

if not exist "lib\maven-repo" (
    echo ERROR: lib\maven-repo not found. This repo must contain pre-cached dependencies.
    exit /b 1
)
echo   Offline Maven repo: OK
echo.

REM --- Step 1: Build backend & frontend JARs (offline) ---
echo [Step 1] Building backend and frontend JARs (offline mode)...
call mvn clean package -DskipTests -o -Dmaven.repo.local=lib\maven-repo -Djavafx.platform=win
if errorlevel 1 (
    echo ERROR: Maven build failed.
    exit /b 1
)
echo   Build: SUCCESS
echo.

REM --- Step 2: Create bundled JRE with jlink ---
echo [Step 2] Creating bundled JRE with jlink...
if exist build\runtime rmdir /s /q build\runtime
jlink --module-path "%JAVA_HOME%\jmods" ^
      --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.management ^
      --output build\runtime ^
      --strip-debug --compress=2 --no-header-files --no-man-pages
if errorlevel 1 (
    echo ERROR: jlink failed. Ensure JAVA_HOME points to JDK 21.
    exit /b 1
)
echo   Bundled JRE: SUCCESS
echo.

REM --- Step 3: Stage application files ---
echo [Step 3] Staging application files...
if exist build\app rmdir /s /q build\app
mkdir build\app
mkdir build\app\data
mkdir build\app\logs

copy backend\target\emms-backend-1.0.0.jar build\app\ >nul
copy frontend\target\emms-frontend-1.0.0.jar build\app\ >nul
copy packaging\emms-launcher.bat build\app\ >nul
copy packaging\update.bat build\app\ >nul
copy packaging\rollback.bat build\app\ >nul
xcopy /E /I /Q build\runtime build\app\runtime >nul
echo   Staging: SUCCESS
echo.

REM --- Step 4: Create installer ---
echo [Step 4] Creating installer...
if not exist dist mkdir dist

REM Try Inno Setup first
where iscc >nul 2>&1
if not errorlevel 1 (
    echo   Using Inno Setup...
    iscc /Odist packaging\emms-installer.iss
    if errorlevel 1 (
        echo   WARNING: Inno Setup build failed, trying jpackage fallback...
        goto :jpackage
    )
    echo.
    echo =========================================
    echo  SUCCESS: dist\EMMS-Lite-Setup-1.0.0.exe
    echo =========================================
    goto :done
)

:jpackage
echo   Inno Setup not found, trying jpackage...
where jpackage >nul 2>&1
if errorlevel 1 (
    echo   WARNING: jpackage not found either. Skipping installer creation.
    echo   Your staged app is ready at: build\app\
    echo   You can run it with: build\app\emms-launcher.bat
    goto :done
)

REM Check for WiX (needed for MSI)
where candle >nul 2>&1
if not errorlevel 1 (
    echo   Using jpackage (MSI via WiX)...
    jpackage --type msi ^
             --name "EMMS Lite" ^
             --app-version 1.0.0 ^
             --vendor "Aircraft" ^
             --input build\app ^
             --main-jar emms-frontend-1.0.0.jar ^
             --main-class com.aircraft.emms.ui.EmmsLauncher ^
             --runtime-image build\runtime ^
             --dest dist ^
             --win-dir-chooser --win-menu --win-shortcut
    echo.
    echo =========================================
    echo  SUCCESS: dist\EMMS Lite-1.0.0.msi
    echo =========================================
) else (
    echo   Using jpackage (EXE)...
    jpackage --type exe ^
             --name "EMMS Lite" ^
             --app-version 1.0.0 ^
             --vendor "Aircraft" ^
             --input build\app ^
             --main-jar emms-frontend-1.0.0.jar ^
             --main-class com.aircraft.emms.ui.EmmsLauncher ^
             --runtime-image build\runtime ^
             --dest dist ^
             --win-dir-chooser --win-menu --win-shortcut
    echo.
    echo =========================================
    echo  SUCCESS: Check dist\ for installer
    echo =========================================
)

:done
echo.
echo Build complete. Output in: dist\
echo.
endlocal
