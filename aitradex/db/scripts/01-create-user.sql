-- Create admin user for aitradex database
-- Usage: psql -U postgres -f db/scripts/01-create-user.sql

-- Drop user if exists (for idempotency)
DROP USER IF EXISTS aitradex_user;

-- Create user with password
CREATE USER aitradex_user WITH PASSWORD 'aitradex_pass';

-- Grant necessary privileges
ALTER USER aitradex_user CREATEDB;

-- Note: Additional privileges will be granted when database is created
-- The user will be granted ALL privileges on the aitradexdb database
