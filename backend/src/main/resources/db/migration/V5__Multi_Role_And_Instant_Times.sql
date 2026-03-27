-- V5__Multi_Role_And_Instant_Times.sql
-- 1. Add 'roles' TEXT column (comma-separated) to users for multi-role support
-- 2. Populate roles from existing single role
-- 3. Sortie times already stored as BIGINT - compatible with Instant

-- Add roles column (comma-separated text)
ALTER TABLE users ADD COLUMN roles TEXT;

-- Migrate existing role data into roles column
UPDATE users SET roles = role;
