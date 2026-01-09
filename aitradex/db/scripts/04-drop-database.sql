-- Drop database for aitradex application
-- Usage: psql -U postgres -f db/scripts/04-drop-database.sql
-- WARNING: This will permanently delete all data in the database

-- Terminate all connections to the database
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'aitradexdb' AND pid <> pg_backend_pid();

-- Drop database if exists
DROP DATABASE IF EXISTS aitradexdb;
