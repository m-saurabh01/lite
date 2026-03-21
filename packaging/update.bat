@echo off
REM ============================================================
REM EMMS Lite - Safe Update Script
REM ============================================================
REM This script safely updates the application binaries while
REM preserving all user data (database, uploads, config, logs).
REM
REM Usage: update.bat <path-to-update-package>
REM
REM The update package should be a ZIP containing:
REM   /emms-backend.jar   - New backend JAR
REM   /emms-frontend.jar  - New frontend JAR
REM   /runtime/           - (optional) Updated bundled JRE
REM   /version.txt        - New version number
REM   /migrate/           - (optional) SQL migration scripts
REM ============================================================

setlocal enabledelayedexpansion

if "%~1"=="" (
    echo Usage: update.bat ^<path-to-update-package.zip^>
    exit /b 1
)

set UPDATE_ZIP=%~1
set APP_DIR=%~dp0
set BACKUP_DIR=%APP_DIR%backups\%date:~-4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%

echo ============================================================
echo  EMMS Lite - Safe Update
echo ============================================================
echo.
echo Update package: %UPDATE_ZIP%
echo Application dir: %APP_DIR%
echo.

REM --- Step 1: Pre-flight checks ---
echo [1/6] Pre-flight checks...
if not exist "%UPDATE_ZIP%" (
    echo ERROR: Update package not found: %UPDATE_ZIP%
    exit /b 1
)

REM Check if app is running
netstat -aon | findstr :8095 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo WARNING: Application appears to be running on port 8095.
    echo Please close the application before updating.
    set /p CONTINUE="Continue anyway? (y/n): "
    if /i not "!CONTINUE!"=="y" exit /b 1
)

REM --- Step 2: Backup database ---
echo.
echo [2/6] Backing up database...
mkdir "%BACKUP_DIR%" 2>nul
if exist "%APP_DIR%data\app.db" (
    copy /y "%APP_DIR%data\app.db" "%BACKUP_DIR%\app.db" >nul
    echo Database backed up to: %BACKUP_DIR%\app.db
) else (
    echo No database found to backup.
)

REM Backup current version info
if exist "%APP_DIR%version.txt" (
    copy /y "%APP_DIR%version.txt" "%BACKUP_DIR%\version.txt.old" >nul
)

REM --- Step 3: Backup existing binaries ---
echo.
echo [3/6] Backing up existing binaries...
if exist "%APP_DIR%emms-backend.jar" (
    copy /y "%APP_DIR%emms-backend.jar" "%BACKUP_DIR%\emms-backend.jar.old" >nul
)
if exist "%APP_DIR%emms-frontend.jar" (
    copy /y "%APP_DIR%emms-frontend.jar" "%BACKUP_DIR%\emms-frontend.jar.old" >nul
)

REM --- Step 4: Extract update ---
echo.
echo [4/6] Extracting update package...
set TEMP_EXTRACT=%APP_DIR%_update_temp
if exist "%TEMP_EXTRACT%" rmdir /s /q "%TEMP_EXTRACT%"
mkdir "%TEMP_EXTRACT%"

powershell -command "Expand-Archive -Path '%UPDATE_ZIP%' -DestinationPath '%TEMP_EXTRACT%' -Force"
if errorlevel 1 (
    echo ERROR: Failed to extract update package.
    echo Rolling back...
    rmdir /s /q "%TEMP_EXTRACT%" 2>nul
    exit /b 1
)

REM --- Step 5: Apply update (ONLY binaries, NOT data) ---
echo.
echo [5/6] Applying update...

REM Copy new JARs
if exist "%TEMP_EXTRACT%\emms-backend.jar" (
    copy /y "%TEMP_EXTRACT%\emms-backend.jar" "%APP_DIR%emms-backend.jar" >nul
    echo Updated: emms-backend.jar
)
if exist "%TEMP_EXTRACT%\emms-frontend.jar" (
    copy /y "%TEMP_EXTRACT%\emms-frontend.jar" "%APP_DIR%emms-frontend.jar" >nul
    echo Updated: emms-frontend.jar
)

REM Copy new version file
if exist "%TEMP_EXTRACT%\version.txt" (
    copy /y "%TEMP_EXTRACT%\version.txt" "%APP_DIR%version.txt" >nul
)

REM Update bundled JRE if new one is provided
if exist "%TEMP_EXTRACT%\runtime" (
    echo Updating bundled JRE...
    if exist "%APP_DIR%runtime" rmdir /s /q "%APP_DIR%runtime"
    xcopy /e /i /q /y "%TEMP_EXTRACT%\runtime" "%APP_DIR%runtime" >nul
    echo Updated: bundled JRE
)

REM Handle migration scripts (if any)
if exist "%TEMP_EXTRACT%\migrate" (
    echo Found migration scripts. They will be applied on next startup.
    if not exist "%APP_DIR%data\pending-migrations" mkdir "%APP_DIR%data\pending-migrations"
    xcopy /y /q "%TEMP_EXTRACT%\migrate\*" "%APP_DIR%data\pending-migrations\" >nul
)

REM --- Step 6: Cleanup ---
echo.
echo [6/6] Cleaning up...
rmdir /s /q "%TEMP_EXTRACT%" 2>nul

echo.
echo ============================================================
echo  Update completed successfully!
echo.
echo  PRESERVED (untouched):
echo    - data\app.db            (database)
echo    - data\uploads\          (uploaded files)
echo    - config\                (configuration)
echo    - logs\                  (log files)
echo.
echo  UPDATED:
echo    - emms-backend.jar       (backend binaries)
echo    - emms-frontend.jar      (frontend binaries)
echo    - runtime\               (bundled JRE, if provided)
echo    - version.txt            (version info)
echo.
echo  BACKUP saved to: %BACKUP_DIR%
echo.
echo  You can now start the application.
echo ============================================================

endlocal
