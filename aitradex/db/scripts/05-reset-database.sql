-- Reset database: drop and recreate aitradexdb
-- Usage: psql -U postgres -f db/scripts/05-reset-database.sql
-- This script is idempotent and can be run multiple times safely

-- Step 1: Terminate all connections to the database
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'aitradexdb' AND pid <> pg_backend_pid();

-- Step 2: Drop database if exists
DROP DATABASE IF EXISTS aitradexdb;

-- Step 3: Create database
CREATE DATABASE aitradexdb
    WITH OWNER = aitradex_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Step 4: Grant all privileges to aitradex_user
GRANT ALL PRIVILEGES ON DATABASE aitradexdb TO aitradex_user;

-- Step 5: Connect to the new database and grant schema privileges
\c aitradexdb

-- Grant privileges on public schema
GRANT ALL ON SCHEMA public TO aitradex_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO aitradex_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO aitradex_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO aitradex_user;

-- Note: After running this script, run Liquibase migrations to create tables:
-- The application will automatically run Liquibase migrations on startup,
-- or you can run them manually using: mvn liquibase:update
