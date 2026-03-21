@echo off
REM ============================================================
REM EMMS Lite - Rollback Script
REM ============================================================
REM Restores application from a backup created during update.
REM
REM Usage: rollback.bat <backup-directory>
REM Example: rollback.bat C:\AircraftApp\backups\20260321_093000
REM ============================================================

setlocal

if "%~1"=="" (
    echo Usage: rollback.bat ^<backup-directory^>
    echo.
    echo Available backups:
    dir /b /ad "%~dp0backups" 2>nul || echo   (no backups found)
    exit /b 1
)

set BACKUP_DIR=%~1
set APP_DIR=%~dp0

if not exist "%BACKUP_DIR%" (
    echo ERROR: Backup directory not found: %BACKUP_DIR%
    exit /b 1
)

echo ============================================================
echo  EMMS Lite - Rollback
echo ============================================================
echo.
echo Rolling back from: %BACKUP_DIR%
echo.

REM Restore binaries
if exist "%BACKUP_DIR%\emms-backend.jar.old" (
    copy /y "%BACKUP_DIR%\emms-backend.jar.old" "%APP_DIR%emms-backend.jar" >nul
    echo Restored: emms-backend.jar
)
if exist "%BACKUP_DIR%\emms-frontend.jar.old" (
    copy /y "%BACKUP_DIR%\emms-frontend.jar.old" "%APP_DIR%emms-frontend.jar" >nul
    echo Restored: emms-frontend.jar
)

REM Restore database if needed
set /p RESTORE_DB="Restore database from backup? (y/n): "
if /i "%RESTORE_DB%"=="y" (
    if exist "%BACKUP_DIR%\app.db" (
        copy /y "%BACKUP_DIR%\app.db" "%APP_DIR%data\app.db" >nul
        echo Restored: database
    )
)

echo.
echo Rollback complete. Start the application to verify.
echo ============================================================

endlocal
