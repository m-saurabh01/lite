@echo off
REM ============================================================
REM EMMS Lite - Application Launcher
REM ============================================================
REM This script starts the backend server, waits for it to be
REM ready, then launches the JavaFX frontend.
REM
REM The app ships with a bundled JRE (Java 21) at runtime\.
REM No system Java installation is required.
REM ============================================================

setlocal

set APP_DIR=%~dp0
set DATA_DIR=%APP_DIR%data
set CONFIG_DIR=%APP_DIR%config
set LOG_DIR=%APP_DIR%logs

REM --- Resolve Java executables from bundled runtime ---
set JAVA_EXE=
set JAVAW_EXE=
if exist "%APP_DIR%runtime\bin\java.exe" (
    set "JAVA_EXE=%APP_DIR%runtime\bin\java.exe"
    set "JAVAW_EXE=%APP_DIR%runtime\bin\javaw.exe"
) else (
    echo ERROR: Bundled JRE not found at %APP_DIR%runtime\bin\
    echo The application install may be corrupted. Please reinstall.
    pause
    exit /b 1
)

REM Ensure data directories exist
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"
if not exist "%DATA_DIR%\uploads" mkdir "%DATA_DIR%\uploads"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM --- Auto-backup database before launch ---
if exist "%DATA_DIR%\app.db" (
    echo Backing up database...
    copy /y "%DATA_DIR%\app.db" "%DATA_DIR%\app.db.backup" >nul 2>&1
)

REM --- Start Backend ---
echo Starting EMMS backend server...
set EMMS_DATA_DIR=%DATA_DIR%
start "" /b "%JAVAW_EXE%" -Xmx512m -jar "%APP_DIR%emms-backend.jar" --spring.config.additional-location=file:%CONFIG_DIR%\application.properties --app.data-dir=%DATA_DIR% > "%LOG_DIR%\backend-start.log" 2>&1

REM --- Wait for backend to be ready ---
echo Waiting for backend...
set /a RETRY=0
:wait_loop
if %RETRY% geq 30 (
    echo ERROR: Backend failed to start within 30 seconds.
    echo Check logs at: %LOG_DIR%\backend-start.log
    pause
    exit /b 1
)

timeout /t 1 /nobreak >nul
curl -s -o nul http://127.0.0.1:8095/api/health
if %errorlevel% equ 0 (
    echo Backend is ready.
    goto :start_frontend
)
set /a RETRY+=1
goto :wait_loop

:start_frontend
REM --- Start Frontend ---
echo Starting EMMS Lite...
"%JAVA_EXE%" -Xmx512m -jar "%APP_DIR%emms-frontend.jar"

REM --- On frontend close, shutdown backend ---
echo Shutting down backend...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8095 ^| findstr LISTENING') do (
    taskkill /PID %%a /F >nul 2>&1
)

echo EMMS Lite closed.
endlocal
