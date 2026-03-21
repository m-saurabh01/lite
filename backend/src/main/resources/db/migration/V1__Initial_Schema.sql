-- V1__Initial_Schema.sql
-- EMMS Lite Database Schema v1.0.0

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    service_id TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('ADMIN', 'CAPTAIN', 'PILOT')),
    security_question TEXT,
    security_answer TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at TEXT DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS sorties (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sortie_number TEXT NOT NULL UNIQUE,
    aircraft_type TEXT NOT NULL,
    aircraft_number TEXT NOT NULL,
    captain_id INTEGER,
    pilot_id INTEGER,
    scheduled_date TEXT NOT NULL,
    scheduled_start TEXT,
    scheduled_end TEXT,
    status TEXT NOT NULL DEFAULT 'CREATED' CHECK(status IN ('CREATED','ASSIGNED','ACCEPTED','REJECTED','IN_PROGRESS','COMPLETED','CANCELLED')),
    remarks TEXT,
    created_by_id INTEGER,
    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (captain_id) REFERENCES users(id),
    FOREIGN KEY (pilot_id) REFERENCES users(id),
    FOREIGN KEY (created_by_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS flight_log_books (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sortie_id INTEGER,
    aircraft_type TEXT NOT NULL,
    aircraft_number TEXT NOT NULL,
    pilot_id INTEGER NOT NULL,
    actual_takeoff_time TEXT,
    actual_landing_time TEXT,
    duration_minutes INTEGER,
    remarks TEXT,
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED')),
    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (sortie_id) REFERENCES sorties(id),
    FOREIGN KEY (pilot_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS meter_definitions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    meter_name TEXT NOT NULL,
    aircraft_type TEXT,
    mandatory INTEGER NOT NULL DEFAULT 0,
    display_order INTEGER DEFAULT 0,
    unit_of_measure TEXT,
    active INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS meter_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flb_id INTEGER NOT NULL,
    meter_definition_id INTEGER,
    meter_name TEXT NOT NULL,
    meter_value TEXT NOT NULL,
    previous_value TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (flb_id) REFERENCES flight_log_books(id),
    FOREIGN KEY (meter_definition_id) REFERENCES meter_definitions(id)
);

CREATE TABLE IF NOT EXISTS xml_import_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    xml_version TEXT,
    imported_by_id INTEGER,
    records_imported INTEGER DEFAULT 0,
    records_failed INTEGER DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    imported_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (imported_by_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_service_id TEXT,
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id INTEGER,
    details TEXT,
    ip_address TEXT,
    timestamp TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_service_id ON users(service_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_sorties_number ON sorties(sortie_number);
CREATE INDEX IF NOT EXISTS idx_sorties_date ON sorties(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_sorties_pilot ON sorties(pilot_id);
CREATE INDEX IF NOT EXISTS idx_sorties_captain ON sorties(captain_id);
CREATE INDEX IF NOT EXISTS idx_sorties_status ON sorties(status);
CREATE INDEX IF NOT EXISTS idx_flb_sortie ON flight_log_books(sortie_id);
CREATE INDEX IF NOT EXISTS idx_flb_pilot ON flight_log_books(pilot_id);
CREATE INDEX IF NOT EXISTS idx_meter_entries_flb ON meter_entries(flb_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_service_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);
