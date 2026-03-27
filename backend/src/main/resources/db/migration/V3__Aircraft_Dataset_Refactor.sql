-- V3__Aircraft_Dataset_Refactor.sql
-- Introduces AircraftDataSet model: one active aircraft at a time.
-- Adds assets, snags tables. Links existing tables to dataset.

-- ============================================================
-- 1. aircraft_data_sets
-- ============================================================
CREATE TABLE IF NOT EXISTS aircraft_data_sets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_num TEXT NOT NULL UNIQUE,
    aircraft_name TEXT NOT NULL,
    aircraft_type TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 0,
    imported_at BIGINT,
    created_at BIGINT,
    updated_at BIGINT
);

-- ============================================================
-- 2. assets (component hierarchy)
-- ============================================================
CREATE TABLE IF NOT EXISTS assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_num TEXT NOT NULL,
    name TEXT NOT NULL,
    parent_asset_num TEXT,
    dataset_id INTEGER NOT NULL,
    created_at BIGINT,
    FOREIGN KEY (dataset_id) REFERENCES aircraft_data_sets(id),
    UNIQUE(asset_num, dataset_id)
);

CREATE INDEX IF NOT EXISTS idx_assets_dataset ON assets(dataset_id);
CREATE INDEX IF NOT EXISTS idx_assets_parent ON assets(parent_asset_num, dataset_id);

-- ============================================================
-- 3. snags
-- ============================================================
CREATE TABLE IF NOT EXISTS snags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_num TEXT NOT NULL,
    description TEXT NOT NULL,
    reported_by TEXT NOT NULL,
    reported_at BIGINT NOT NULL,
    dataset_id INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','CLOSED')),
    created_at BIGINT,
    FOREIGN KEY (dataset_id) REFERENCES aircraft_data_sets(id)
);

CREATE INDEX IF NOT EXISTS idx_snags_dataset ON snags(dataset_id);

-- ============================================================
-- 4. Add dataset_id to existing tables
-- ============================================================
ALTER TABLE users ADD COLUMN dataset_id INTEGER REFERENCES aircraft_data_sets(id);
ALTER TABLE meter_definitions ADD COLUMN dataset_id INTEGER REFERENCES aircraft_data_sets(id);
ALTER TABLE meter_definitions ADD COLUMN meter_num TEXT;
ALTER TABLE meter_definitions ADD COLUMN meter_type TEXT DEFAULT 'CONTINUOUS' CHECK(meter_type IN ('CONTINUOUS','NON_CONTINUOUS'));
ALTER TABLE meter_definitions ADD COLUMN asset_num TEXT;
ALTER TABLE meter_definitions ADD COLUMN initial_value TEXT DEFAULT '0';
ALTER TABLE sorties ADD COLUMN dataset_id INTEGER REFERENCES aircraft_data_sets(id);
ALTER TABLE flight_log_books ADD COLUMN dataset_id INTEGER REFERENCES aircraft_data_sets(id);

-- ============================================================
-- 5. Indexes for dataset filtering
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_users_dataset ON users(dataset_id);
CREATE INDEX IF NOT EXISTS idx_meters_dataset ON meter_definitions(dataset_id);
CREATE INDEX IF NOT EXISTS idx_sorties_dataset ON sorties(dataset_id);
CREATE INDEX IF NOT EXISTS idx_flb_dataset ON flight_log_books(dataset_id);

-- ============================================================
-- 6. Update Role CHECK to include TECHNICIAN, MECHANIC
-- ============================================================
-- SQLite does not support ALTER CHECK, but the JPA layer will handle validation.
-- Existing ADMIN user (dataset_id=NULL) remains the system admin.
