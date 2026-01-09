# Database Setup Scripts

This directory contains SQL scripts for managing the aitradex PostgreSQL database.

## Prerequisites

- PostgreSQL installed and running locally
- `psql` command-line tool available
- Access to PostgreSQL superuser (typically `postgres` user)

## Scripts

### 1. Create User (`01-create-user.sql`)

Creates the `aitradex_user` database user with password `aitradex_pass`.

```bash
psql -U postgres -f db/scripts/01-create-user.sql
```

### 2. Drop User (`02-drop-user.sql`)

Drops the `aitradex_user` database user.

```bash
psql -U postgres -f db/scripts/02-drop-user.sql
```

### 3. Create Database (`03-create-database.sql`)

Creates the `aitradexdb` database and grants all privileges to `aitradex_user`.

**Note:** Ensure `aitradex_user` exists before running this script.

```bash
psql -U postgres -f db/scripts/03-create-database.sql
```

### 4. Drop Database (`04-drop-database.sql`)

Drops the `aitradexdb` database. **WARNING:** This permanently deletes all data.

```bash
psql -U postgres -f db/scripts/04-drop-database.sql
```

### 5. Reset Database (`05-reset-database.sql`)

Idempotent script that drops and recreates the `aitradexdb` database.
This is useful for development when you need a fresh database.

```bash
psql -U postgres -f db/scripts/05-reset-database.sql
```

## Complete Setup Workflow

For a fresh setup:

```bash
# 1. Create user
psql -U postgres -f db/scripts/01-create-user.sql

# 2. Create database
psql -U postgres -f db/scripts/03-create-database.sql

# 3. Start the application (Liquibase will create tables automatically)
cd aitradex-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Quick Reset (Development)

To reset the database during development:

```bash
psql -U postgres -f db/scripts/05-reset-database.sql
# Then restart the application - Liquibase will recreate all tables
```

## Security Notes

- These scripts use default passwords (`aitradex_pass`) suitable for local development only
- For production, use strong passwords and environment variables
- Never commit production credentials to version control
