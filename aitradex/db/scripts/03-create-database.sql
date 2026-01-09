-- Create database for aitradex application
-- Usage: psql -U postgres -f db/scripts/03-create-database.sql
-- Note: Ensure aitradex_user exists before running this script

-- Drop database if exists (for idempotency)
-- Note: This will disconnect any existing connections
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'aitradexdb' AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS aitradexdb;

-- Create database
CREATE DATABASE aitradexdb
    WITH OWNER = aitradex_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Grant all privileges to aitradex_user
GRANT ALL PRIVILEGES ON DATABASE aitradexdb TO aitradex_user;

-- Connect to the new database and grant schema privileges
\c aitradexdb

-- Grant privileges on public schema
GRANT ALL ON SCHEMA public TO aitradex_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO aitradex_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO aitradex_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO aitradex_user;
