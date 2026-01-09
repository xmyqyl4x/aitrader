# Local Development Setup Guide

This guide provides step-by-step instructions for setting up and running the aitradex application locally without containers.

## Prerequisites

1. **Java 21** - Install JDK 21
2. **Maven 3.9+** - Install Maven
3. **Node.js 18+ and npm** - Install Node.js
4. **PostgreSQL 16** - Install PostgreSQL locally

## Database Setup

### 1. Create Database User

```bash
psql -U postgres -f db/scripts/01-create-user.sql
```

This creates:
- User: `aitradex_user`
- Password: `aitradex_pass`

### 2. Create Database

```bash
psql -U postgres -f db/scripts/03-create-database.sql
```

This creates:
- Database: `aitradexdb`
- Grants all privileges to `aitradex_user`

**OR** use the reset script for a fresh setup:

```bash
psql -U postgres -f db/scripts/05-reset-database.sql
```

### 3. Verify Database

```bash
psql -U aitradex_user -d aitradexdb -c "SELECT version();"
```

## Build the Application

**⚠️ IMPORTANT: You must run all commands from the individual project directories, NOT from the root directory.**

### Backend

**From `aitradex-service` directory:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn clean install
```

### Frontend

**From `aitradex-ui` directory:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm install --legacy-peer-deps
```

## Run the Application

### 1. Start Backend

**⚠️ Must run from `aitradex-service` directory:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend will:
- Connect to PostgreSQL at `localhost:5432/aitradexdb`
- Run Liquibase migrations automatically
- Start on `http://localhost:8080`

### 2. Start Frontend (in a separate terminal)

**⚠️ Must run from `aitradex-ui` directory:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm start
```

The frontend will:
- Start on `http://localhost:4200`
- Proxy API calls to `http://localhost:8080/api`

## Verify Installation

1. **Backend Health Check:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Swagger UI:**
   Open `http://localhost:8080/api/swagger-ui.html`

3. **Frontend:**
   Open `http://localhost:4200`

## Configuration

### Database Configuration

Default values (can be overridden via environment variables):

- **URL**: `jdbc:postgresql://localhost:5432/aitradexdb`
- **Username**: `aitradex_user`
- **Password**: `aitradex_pass`

Environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### E*TRADE Configuration

Default values (can be overridden via environment variables):

- `ETRADE_CONSUMER_KEY` (default: `a83b0321f09e97fc8f4315ad5fbcd489`)
- `ETRADE_CONSUMER_SECRET` (default: `c4d304698d156d4c3681c73de0c4e400060cac46ee1504259b324695daa77dd4`)
- `ETRADE_ENVIRONMENT` (default: `SANDBOX`)
- `ETRADE_CALLBACK_URL` (default: `http://localhost:4200/etrade-review-trade/callback`)

## Troubleshooting

### Database Connection Issues

1. **Check PostgreSQL is running:**
   ```bash
   pg_isready
   # or
   psql -U postgres -c "SELECT 1"
   ```

2. **Verify database exists:**
   ```bash
   psql -U postgres -l | grep aitradexdb
   ```

3. **Check user permissions:**
   ```bash
   psql -U postgres -c "\du aitradex_user"
   ```

### Port Conflicts

- **Backend port 8080:** Set `SERVER_PORT` environment variable
- **Frontend port 4200:** Use `ng serve --port <port>` or set `PORT` environment variable

### Liquibase Migration Issues

1. Check application logs for migration errors
2. Verify database user has CREATE/ALTER privileges
3. Reset database if needed:
   ```bash
   psql -U postgres -f db/scripts/05-reset-database.sql
   ```

## Quick Reference

### Reset Database (Development)

```bash
psql -U postgres -f db/scripts/05-reset-database.sql
# Then restart the application - Liquibase will recreate all tables
```

### Run Tests

**Backend:**
```bash
cd aitradex-service
mvn test
```

**Frontend:**
```bash
cd aitradex-ui
npm test
```

### Build for Production

**Backend:**
```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn clean package -DskipTests
# JAR will be in: target/aitradex-service-*.jar
```

**Frontend:**
```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm run build
# Build artifacts in: dist/aitradex-ui/
```

## Environment Variables

Create a `.env` file (not committed) or set environment variables:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aitradexdb
export SPRING_DATASOURCE_USERNAME=aitradex_user
export SPRING_DATASOURCE_PASSWORD=aitradex_pass

# E*TRADE
export ETRADE_CONSUMER_KEY=your_key_here
export ETRADE_CONSUMER_SECRET=your_secret_here
export ETRADE_ENVIRONMENT=SANDBOX

# Alpha Vantage
export ALPHA_VANTAGE_API_KEY=your_key_here
```

## Next Steps

1. Set up database (see Database Setup above)
2. Build the application:
   - Backend: `cd aitradex-service && mvn clean install`
   - Frontend: `cd aitradex-ui && npm install --legacy-peer-deps`
3. Run backend (from `aitradex-service` directory):
   ```bash
   cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
4. Run frontend (from `aitradex-ui` directory, in separate terminal):
   ```bash
   cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
   npm start
   ```
5. Access application at `http://localhost:4200`

**Important:** You cannot run the application from the root directory. Each component must be run from its respective directory (`aitradex-service` or `aitradex-ui`).
