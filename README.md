# EMMS Lite - Aircraft Operations Management System

A production-grade, offline-first Windows desktop application for aircraft operations management including sortie lifecycle, flight log book (FLB), meter tracking, and XML data import.

## Architecture

```
[ JavaFX UI (Frontend) ]
        ↓ REST API via localhost:8095
[ Spring Boot Backend (Embedded Server) ]
        ↓
[ SQLite Database + File Storage ]
```

### Technology Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 3.2.5, Java 21 |
| Frontend | JavaFX 21, FXML |
| Database | SQLite (file-based) |
| Migrations | Flyway |
| Security | BCrypt, Token-based auth |
| Packaging | jpackage + jlink (bundled JRE, no system Java needed) |

## Project Structure

```
emmslite-win/
├── pom.xml                     # Parent Maven POM
├── backend/                    # Spring Boot backend
│   ├── pom.xml
│   └── src/main/java/com/aircraft/emms/
│       ├── EmmsBackendApplication.java
│       ├── config/             # App & web configuration
│       ├── controller/         # REST controllers
│       ├── dto/                # Data transfer objects
│       ├── entity/             # JPA entities
│       ├── repository/         # Spring Data repositories
│       ├── security/           # Auth, tokens, filters
│       ├── service/            # Business logic
│       └── xmlimport/          # XML import engine
├── frontend/                   # JavaFX frontend
│   ├── pom.xml
│   └── src/main/java/com/aircraft/emms/ui/
│       ├── EmmsApplication.java
│       ├── EmmsLauncher.java
│       ├── controller/         # FXML controllers
│       ├── model/              # Client-side DTOs
│       └── service/            # API client layer
├── packaging/                  # Build & deploy scripts
│   ├── build-installer.bat     # Windows installer builder
│   ├── build.sh                # Dev build (macOS/Linux)
│   ├── emms-launcher.bat       # Application launcher
│   ├── update.bat              # Safe update script
│   └── rollback.bat            # Rollback script
└── docs/                       # Documentation
```

## Features

### Core Modules
- **Authentication**: BCrypt passwords, security questions, token-based sessions
- **User Management**: CRUD operations, role-based access (Admin/Captain/Pilot)
- **Sortie Management**: Full lifecycle (Create → Assign → Accept/Reject → Complete)
- **Flight Log Book (FLB)**: Create, edit, submit, approve with auto-duration calculation
- **Meter Tracking**: Dynamic meter entries with mandatory validation
- **XML Import**: ZIP-based bulk import with versioning and audit trail
- **Audit Logging**: Who-did-what tracking for all operations

### Security
- BCrypt-12 password hashing
- Role-based access control (RBAC) on both frontend and backend
- Token-based authentication (Bearer tokens)
- XXE prevention in XML parsing
- Input validation at UI + backend layers
- Localhost-only API binding (127.0.0.1)

### Roles & Permissions

| Action | Admin | Captain | Pilot |
|--------|-------|---------|-------|
| Import XML | ✅ | ❌ | ❌ |
| Manage Users | ✅ | ❌ | ❌ |
| Create Sortie | ✅ | ✅ | ❌ |
| Assign Pilot | ✅ | ✅ | ❌ |
| Accept/Reject Sortie | ❌ | ❌ | ✅ |
| Fill FLB | ✅ | ✅ | ✅ |
| Approve FLB | ✅ | ✅ | ❌ |

## Quick Start

See [INSTALL.md](docs/INSTALL.md) for full setup instructions.

### Prerequisites (Development Only)
- JDK 21+ (only needed for building; end users need nothing installed)
- Maven 3.9+

### Development Run

```bash
# 1. Build backend
cd backend && mvn clean package -DskipTests

# 2. Start backend
java -jar target/emms-backend-1.0.0.jar

# 3. Build frontend (separate terminal)
cd frontend && mvn clean package -DskipTests

# 4. Start frontend
mvn javafx:run -f frontend/pom.xml
```

### Default Login
- **Service ID**: `ADMIN001`
- **Password**: `Admin@123`

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/auth/login | Public | Login |
| POST | /api/auth/logout | Bearer | Logout |
| GET | /api/auth/security-question | Public | Get security question |
| POST | /api/auth/reset-password | Public | Reset password |
| GET | /api/users | Bearer | List all users |
| POST | /api/users/manage/create | Admin | Create user |
| PUT | /api/users/manage/{id} | Admin | Update user |
| DELETE | /api/users/manage/{id} | Admin | Deactivate user |
| POST | /api/sorties/create | Captain+ | Create sortie |
| POST | /api/sorties/assign | Captain+ | Assign pilot |
| POST | /api/sorties/{id}/accept | Pilot | Accept sortie |
| POST | /api/sorties/{id}/reject | Pilot | Reject sortie |
| GET | /api/sorties | Bearer | List sorties |
| POST | /api/flb | Bearer | Create FLB |
| PUT | /api/flb/{id} | Bearer | Update FLB |
| POST | /api/flb/{id}/submit | Bearer | Submit FLB |
| POST | /api/flb/{id}/approve | Captain+ | Approve FLB |
| POST | /api/xml-import/upload | Admin | Import ZIP |
| GET | /api/health | Public | Health check |

## Future-Proofing

The architecture is designed to support:
1. **Workorder Module** — Link asset maintenance to FLB entries
2. **Life Remaining Calculation** — Modular service replacing DB2 stored procedures
3. **Maximo 7.6 Integration** — Data sync adapter layer for import/export
