-- V2__Default_Admin_User.sql
-- Insert default admin user (password: Admin@123, BCrypt hash with 12 rounds)
-- This password MUST be changed on first login in production

INSERT OR IGNORE INTO users (service_id, password, name, role, security_question, security_answer, active)
VALUES (
    'ADMIN001',
    'PLACEHOLDER_HASH',
    'System Administrator',
    'ADMIN',
    'What is the system default code?',
    'PLACEHOLDER_HASH',
    1
);

-- Insert sample meter definitions
INSERT OR IGNORE INTO meter_definitions (meter_name, aircraft_type, mandatory, display_order, unit_of_measure, active)
VALUES
    ('AIRFRAME_HOURS', NULL, 1, 1, 'Hours', 1),
    ('ENGINE_HOURS', NULL, 1, 2, 'Hours', 1),
    ('LANDINGS', NULL, 1, 3, 'Count', 1),
    ('FUEL_CONSUMED', NULL, 0, 4, 'Liters', 1),
    ('OIL_CONSUMPTION', NULL, 0, 5, 'Liters', 1);
