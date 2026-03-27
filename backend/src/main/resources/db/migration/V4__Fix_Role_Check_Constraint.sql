-- V4__Fix_Role_Check_Constraint.sql
-- SQLite doesn't support ALTER CHECK, so we recreate the users table
-- to allow TECHNICIAN and MECHANIC roles.

-- Step 1: Create new table with updated CHECK
CREATE TABLE users_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    service_id TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('ADMIN', 'CAPTAIN', 'PILOT', 'TECHNICIAN', 'MECHANIC')),
    security_question TEXT,
    security_answer TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    created_at BIGINT,
    updated_at BIGINT,
    dataset_id INTEGER REFERENCES aircraft_data_sets(id)
);

-- Step 2: Copy data
INSERT INTO users_new (id, service_id, password, name, role, security_question, security_answer, active, created_at, updated_at, dataset_id)
SELECT id, service_id, password, name, role, security_question, security_answer, active, created_at, updated_at, dataset_id
FROM users;

-- Step 3: Drop old table
DROP TABLE users;

-- Step 4: Rename new table
ALTER TABLE users_new RENAME TO users;

-- Step 5: Recreate indexes
CREATE INDEX IF NOT EXISTS idx_users_service_id ON users(service_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_dataset ON users(dataset_id);

-- Also fix the FLB status CHECK: need OPEN and CLOSED
CREATE TABLE flight_log_books_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sortie_id INTEGER REFERENCES sorties(id),
    aircraft_type TEXT NOT NULL,
    aircraft_number TEXT NOT NULL,
    pilot_id INTEGER NOT NULL REFERENCES users(id),
    actual_takeoff_time BIGINT,
    actual_landing_time BIGINT,
    duration_minutes INTEGER,
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'OPEN', 'CLOSED')),
    remarks TEXT,
    created_at BIGINT,
    updated_at BIGINT,
    dataset_id INTEGER REFERENCES aircraft_data_sets(id)
);

INSERT INTO flight_log_books_new (id, sortie_id, aircraft_type, aircraft_number, pilot_id, actual_takeoff_time, actual_landing_time, duration_minutes, status, remarks, created_at, updated_at, dataset_id)
SELECT id, sortie_id, aircraft_type, aircraft_number, pilot_id, actual_takeoff_time, actual_landing_time, duration_minutes, status, remarks, created_at, updated_at, dataset_id
FROM flight_log_books;

DROP TABLE flight_log_books;
ALTER TABLE flight_log_books_new RENAME TO flight_log_books;

CREATE INDEX IF NOT EXISTS idx_flb_pilot ON flight_log_books(pilot_id);
CREATE INDEX IF NOT EXISTS idx_flb_sortie ON flight_log_books(sortie_id);
CREATE INDEX IF NOT EXISTS idx_flb_dataset ON flight_log_books(dataset_id);

-- Fix sortie status CHECK: add CLOSED, also add has_clash and clash_details columns
CREATE TABLE sorties_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sortie_number TEXT NOT NULL,
    aircraft_type TEXT NOT NULL,
    aircraft_number TEXT NOT NULL,
    captain_id INTEGER NOT NULL REFERENCES users(id),
    pilot_id INTEGER REFERENCES users(id),
    scheduled_date BIGINT NOT NULL,
    scheduled_start BIGINT,
    scheduled_end BIGINT,
    status TEXT NOT NULL DEFAULT 'CREATED' CHECK(status IN ('CREATED', 'ASSIGNED', 'ACCEPTED', 'REJECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'CLOSED')),
    remarks TEXT,
    has_clash INTEGER NOT NULL DEFAULT 0,
    clash_details TEXT,
    created_by_id INTEGER REFERENCES users(id),
    created_at BIGINT,
    updated_at BIGINT,
    dataset_id INTEGER REFERENCES aircraft_data_sets(id)
);

INSERT INTO sorties_new (id, sortie_number, aircraft_type, aircraft_number, captain_id, pilot_id, scheduled_date, scheduled_start, scheduled_end, status, remarks, created_by_id, created_at, updated_at, dataset_id)
SELECT id, sortie_number, aircraft_type, aircraft_number, captain_id, pilot_id, scheduled_date, scheduled_start, scheduled_end, status, remarks, created_by_id, created_at, updated_at, dataset_id
FROM sorties;

DROP TABLE sorties;
ALTER TABLE sorties_new RENAME TO sorties;

CREATE INDEX IF NOT EXISTS idx_sorties_captain ON sorties(captain_id);
CREATE INDEX IF NOT EXISTS idx_sorties_pilot ON sorties(pilot_id);
CREATE INDEX IF NOT EXISTS idx_sorties_date ON sorties(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_sorties_dataset ON sorties(dataset_id);
