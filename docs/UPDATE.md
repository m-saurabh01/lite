# EMMS Lite - Safe Update Guide

## Update Philosophy

EMMS Lite follows a **safe update** strategy:

- ✅ **REPLACED** on update: Application binaries (JARs), bundled JRE (runtime/), version file
- ❌ **NEVER deleted**: Database (`data/app.db`), uploads, config, logs
- 🔄 **Auto-backup**: Database is backed up before every update and every launch

---

## Update Methods

### Method 1: Update Script (Recommended)

1. **Download** the update package (ZIP file)
2. **Close** the running application
3. **Run** the update script:

```batch
cd C:\AircraftApp
packaging\update.bat C:\Downloads\emms-update-1.1.0.zip
```

4. **Verify** the update:
   - Check `version.txt` shows new version
   - Start the application
   - Confirm login works

### Method 2: Reinstall with Installer

1. Run the new version's `.exe` installer
2. Choose the **same installation directory**
3. The installer will:
   - Overwrite binaries (JARs) and bundled JRE (`runtime/`)
   - Leave `data/`, `config/`, `logs/` untouched
4. Start the application

### Method 3: Manual Binary Replacement

1. Close the application
2. Backup database:
   ```batch
   copy data\app.db data\app.db.backup
   ```
3. Replace JARs:
   ```batch
   copy /y new-emms-backend.jar emms-backend.jar
   copy /y new-emms-frontend.jar emms-frontend.jar
   ```
4. Update version.txt
5. Start the application

---

## Database Migration

When a new version includes schema changes:

### Automatic (Flyway)
- New migration scripts are in `/resources/db/migration/`
- Flyway runs them automatically on startup
- Migrations are versioned (V1, V2, V3...) and idempotent

### Manual
If migration scripts are included in the update package:
1. They are placed in `data/pending-migrations/`
2. Applied automatically on next backend startup
3. Or applied manually:
   ```bash
   sqlite3 data/app.db < data/pending-migrations/V3__New_Feature.sql
   ```

---

## Rollback

If an update causes issues:

```batch
cd C:\AircraftApp
packaging\rollback.bat backups\20260321_093000
```

This restores:
- Previous application binaries
- Optionally, the database backup

---

## Update Package Format

An update package is a ZIP file containing:

```
emms-update-1.1.0.zip
├── emms-backend.jar          # New backend binary
├── emms-frontend.jar         # New frontend binary
├── runtime/                  # (Optional) Updated bundled JRE
├── version.txt               # New version number
└── migrate/                  # (Optional) SQL migration scripts
    └── V3__New_Feature.sql
```

---

## Pre-Update Checklist

1. ✅ Close the running application
2. ✅ Verify backup exists in `backups/` directory
3. ✅ Note current version from `version.txt`
4. ✅ Review release notes for breaking changes
5. ✅ Run the update
6. ✅ Start application and verify functionality
7. ✅ Check logs for migration errors

---

## Data Safety Guarantees

| Directory | Update Behavior | Backup Strategy |
|-----------|----------------|-----------------|
| `data/app.db` | NEVER touched | Auto-backed up on launch + update |
| `data/uploads/` | NEVER touched | Manual backup recommended |
| `config/` | NEVER touched | Preserved across updates |
| `logs/` | NEVER touched | Rotated automatically |
| `emms-*.jar` | REPLACED | Old binaries backed up |
| `runtime/` | REPLACED | Updated JRE from update package |
| `backups/` | GROW ONLY | Contains all update snapshots |
