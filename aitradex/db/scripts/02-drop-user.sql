-- Drop admin user for aitradex database
-- Usage: psql -U postgres -f db/scripts/02-drop-user.sql

-- Drop user if exists
DROP USER IF EXISTS aitradex_user;
