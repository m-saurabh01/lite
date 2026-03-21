# EMMS Lite - Installation Guide

## Prerequisites

### For Development
- **JDK 21+** (Oracle JDK or Eclipse Temurin)
- **Maven 3.9+**
- **Git**

### For Building Windows Installer
- **Windows 10/11** (64-bit)
- **JDK 21+** with `jpackage` tool
- **WiX Toolset 3.x** (for MSI format) — Optional
- **Inno Setup 6** (for EXE format) — Optional

---

## Development Setup

### 1. Clone & Build

```bash
git clone <repository-url> emmslite-win
cd emmslite-win

# Build everything
cd backend && mvn clean package -DskipTests && cd ..
cd frontend && mvn clean package -DskipTests && cd ..
```

### 2. Run Backend

```bash
cd backend
java -jar target/emms-backend-1.0.0.jar
```

The backend starts on `http://127.0.0.1:8095`. It will:
- Create `./data/` directory automatically
- Initialize SQLite database with Flyway migrations
- Create default admin user (ADMIN001 / Admin@123)

Verify: `curl http://127.0.0.1:8095/api/health`

### 3. Run Frontend

```bash
cd frontend
mvn javafx:run
```

Or run the JAR directly (using bundled runtime or system Java):
```bash
# If bundled runtime exists:
./build/app/runtime/bin/java -jar target/emms-frontend-1.0.0.jar

# Otherwise (dev with system Java):
java -jar target/emms-frontend-1.0.0.jar
```

### 4. Login

Use the default admin credentials:
- **Service ID**: `ADMIN001`
- **Password**: `Admin@123`

---

## Building the Windows Installer

### Option 1: jpackage + jlink (Recommended)

```batch
cd packaging
build-installer.bat
```

This will:
1. Build backend and frontend JARs
2. Create a bundled JRE using `jlink` (Java 21 runtime)
3. Stage all files into `build/app/`
4. Run Inno Setup or `jpackage` to create a Windows `.exe` installer
5. Output to `dist/` directory

The resulting installer is **fully self-contained** — end users do not need Java installed.

### Option 2: Manual Packaging

If `jpackage` is not available:

1. Build both modules:
   ```batch
   cd backend && mvn clean package -DskipTests
   cd ..\frontend && mvn clean package -DskipTests
   ```

2. Create installation directory:
   ```
   C:\AircraftApp\
   ├── emms-backend.jar
   ├── emms-frontend.jar
   ├── emms-launcher.bat
   ├── version.txt
   ├── runtime\          (bundled JRE from jlink)
   ├── config\
   │   └── application.properties
   ├── data\           (created at runtime)
   └── logs\           (created at runtime)
   ```

3. Create the bundled JRE:
   ```batch
   jlink --module-path "%JAVA_HOME%\jmods" ^
     --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.management ^
     --output C:\AircraftApp\runtime ^
     --strip-debug --compress zip-6 --no-header-files --no-man-pages
   ```

4. Create a shortcut to `emms-launcher.bat`

---

## Installation on Target Machine

**No Java installation is required.** The installer bundles a Java 21 runtime (JRE) via jlink.

### From .exe Installer

1. Run `EMMS Lite-1.0.0.exe`
2. Follow the installation wizard
3. Choose install directory (default: `C:\AircraftApp`)
4. Launch from Start Menu or Desktop shortcut

### Installation Directory Layout

```
C:\AircraftApp\
├── runtime\                  # Bundled Java 21 JRE (from jlink)
│   └── bin\                  # java.exe / javaw.exe used by launcher
├── emms-backend.jar         # Spring Boot backend (fat JAR)
├── emms-frontend.jar        # JavaFX frontend (fat JAR)
├── emms-launcher.bat        # Application launcher (uses bundled JRE)
├── config\
│   └── application.properties    # Editable configuration
├── data\
│   ├── app.db              # SQLite database (NEVER deleted)
│   └── uploads\
│       └── xml\            # Imported XML files
├── logs\
│   └── emms-backend.log    # Application logs
└── version.txt             # Current version
```

---

## Configuration

Edit `config/application.properties` to customize:

```properties
# Server port (default: 8095)
server.port=8095

# Database location
app.data-dir=./data

# Log level
logging.level.com.aircraft.emms=DEBUG

# Token validity (hours)
app.security.token-validity-hours=24
```

---

## Verifying Installation

1. Start the application via launcher or shortcut
2. Wait for "Backend connected" message on login screen
3. Log in with ADMIN001 / Admin@123
4. Navigate to User Management to create additional users
5. Check `logs/emms-backend.log` for any issues

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Backend unavailable" | Check if port 8095 is free: `netstat -an \| findstr 8095` |
| Database errors | Check `data/app.db` exists and is not locked |
| Login fails | Verify admin user exists in database |
| JavaFX not found | The shaded frontend JAR bundles JavaFX; ensure you use the fat JAR from `target/` |
| Installer won't build | Verify Inno Setup or WiX is installed and `jlink` / `jpackage` are on PATH |
