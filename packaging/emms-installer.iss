; ============================================================
; EMMS Lite - Inno Setup Installer Script
; ============================================================
; Creates a self-contained .exe installer with a bundled
; Java 21 runtime. End users do NOT need Java installed.
;
; Requires Inno Setup 6: https://jrsoftware.org/isinfo.php
; Compile with: ISCC.exe emms-installer.iss
; ============================================================

#define MyAppName "EMMS Lite"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Aircraft Operations"
#define MyAppExeName "emms-launcher.bat"

[Setup]
AppId={{B8F2A0E1-3C4D-4E5F-A6B7-C8D9E0F1A2B3}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName=C:\AircraftApp
DefaultGroupName={#MyAppName}
OutputDir=..\dist
OutputBaseFilename=EMMS-Lite-Setup-{#MyAppVersion}
Compression=lzma
SolidCompression=yes
WizardStyle=modern
; Do NOT uninstall data directory
UninstallFilesDir={app}\uninstall

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; Application binaries
Source: "..\build\app\emms-backend.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\build\app\emms-frontend.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\build\app\emms-launcher.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\build\app\version.txt"; DestDir: "{app}"; Flags: ignoreversion

; Configuration (only if not exists — preserve existing)
Source: "..\build\app\config\application.properties"; DestDir: "{app}\config"; Flags: onlyifdoesntexist

; Bundled JRE (Java 21 runtime created by jlink)
Source: "..\build\app\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs

[Dirs]
; Create persistent directories (never deleted on uninstall)
Name: "{app}\data"; Flags: uninsneveruninstall
Name: "{app}\data\uploads"; Flags: uninsneveruninstall
Name: "{app}\data\uploads\xml"; Flags: uninsneveruninstall
Name: "{app}\config"; Flags: uninsneveruninstall
Name: "{app}\logs"; Flags: uninsneveruninstall
Name: "{app}\backups"; Flags: uninsneveruninstall

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Launch {#MyAppName}"; Flags: nowait postinstall skipifsilent shellexec

[UninstallDelete]
; Only delete application binaries on uninstall, NOT data
Type: files; Name: "{app}\emms-backend.jar"
Type: files; Name: "{app}\emms-frontend.jar"
Type: files; Name: "{app}\emms-launcher.bat"
Type: files; Name: "{app}\version.txt"

[Code]
// Pre-install: backup database if updating
procedure CurStepChanged(CurStep: TSetupStep);
var
  DbFile, BackupFile: String;
begin
  if CurStep = ssInstall then
  begin
    DbFile := ExpandConstant('{app}\data\app.db');
    if FileExists(DbFile) then
    begin
      BackupFile := ExpandConstant('{app}\data\app.db.pre-update-backup');
      FileCopy(DbFile, BackupFile, False);
      Log('Database backed up to: ' + BackupFile);
    end;
  end;
end;
