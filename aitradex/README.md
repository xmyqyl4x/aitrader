# Aitradex

Aitradex is a two-tier application composed of a Spring Boot backend and an Angular frontend, backed by PostgreSQL.

## Components

- **Backend**: `aitradex-service` (Spring Boot 3, Java 21)
- **Frontend**: `aitradex-ui` (Angular 17)
- **Database**: PostgreSQL 16

## Prerequisites

- **Java 21** (for the backend)
- **Maven 3.9+**
- **Node.js 18+ and npm** (for the frontend)
- **PostgreSQL 16** (installed locally)

## Database Setup

Before running the application, set up the PostgreSQL database:

1. **Create database user:**
   ```bash
   psql -U postgres -f db/scripts/01-create-user.sql
   ```

2. **Create database:**
   ```bash
   psql -U postgres -f db/scripts/03-create-database.sql
   ```

   Or use the reset script for a fresh setup:
   ```bash
   psql -U postgres -f db/scripts/05-reset-database.sql
   ```

See `db/scripts/README.md` for detailed database management scripts.

## Build / Install Dependencies

**⚠️ IMPORTANT: You must run build commands from the individual project directories, NOT from the root directory.**

### Backend (Spring Boot)

**From `aitradex-service` directory:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn clean install
```

If you want to skip tests during packaging:

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn clean install -DskipTests
```

Build artifacts are created in `aitradex-service/target/aitradex-service-0.0.1-SNAPSHOT.jar`.

### Frontend (Angular)

**From `aitradex-ui` directory:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm install --legacy-peer-deps
```

Build for production:

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm run build
```

Build artifacts are emitted to `aitradex-ui/dist/aitradex-ui`.

## Tests

### Backend

```bash
cd aitradex-service
mvn test
```

> Note: Integration tests use Testcontainers and require Docker to be running. Unit tests run without Docker.

### Frontend

```bash
cd aitradex-ui
npm test
```

Optional linting:

```bash
npm run lint
```

## Run the Application

### Prerequisites

1. **PostgreSQL must be running locally** on port 5432
2. **Database and user must be created** (see Database Setup above)

### Run Backend

**⚠️ Must run from `aitradex-service` directory:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend will:
- Connect to PostgreSQL at `localhost:5432/aitradexdb`
- Run Liquibase migrations automatically
- Listen on `http://localhost:8080`
- Expose API routes under `/api`

### Run Frontend

**⚠️ Must run from `aitradex-ui` directory:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm start
```

The Angular dev server will:
- Run at `http://localhost:4200/`
- Proxy API calls to `http://localhost:8080/api`

### Development Workflow

1. **Start PostgreSQL** (if not already running)

2. **Start backend** (from `aitradex-service` directory):
   ```bash
   cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **Start frontend** (in a separate terminal, from `aitradex-ui` directory):
   ```bash
   cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
   npm start
   ```

**Note:** You cannot run the application from the root directory (`C:\dev2025\java-projects\devspaces\aitrader\aitradex`). Each component must be run from its respective directory.

## Access the Application

- **UI**: `http://localhost:4200/`
- **API base URL**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **Health check**: `http://localhost:8080/actuator/health`

## Configuration

### Database Configuration

The application is configured to use a local PostgreSQL instance:

- **Database**: `aitradexdb`
- **User**: `aitradex_user`
- **Password**: `aitradex_pass`
- **Host**: `localhost:5432`

These can be overridden via environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### E*TRADE Configuration

E*TRADE credentials are configured in `application.yml` and can be overridden via environment variables:

- `ETRADE_CONSUMER_KEY` (default: `a83b0321f09e97fc8f4315ad5fbcd489`)
- `ETRADE_CONSUMER_SECRET` (default: `c4d304698d156d4c3681c73de0c4e400060cac46ee1504259b324695daa77dd4`)
- `ETRADE_ENVIRONMENT` (default: `SANDBOX`)
- `ETRADE_CALLBACK_URL` (default: `http://localhost:4200/etrade-review-trade/callback`)

### Configuration Files

- **Main config**: `aitradex-service/src/main/resources/application.yml`
- **Dev profile**: `aitradex-service/src/main/resources/application-dev.properties`
- **Prod profile**: `aitradex-service/src/main/resources/application-prod.yml`

## Container Artifacts (Archived)

Container-related files (Dockerfiles, docker-compose.yml, etc.) have been moved to the `attic/` directory for reference. The application now runs as a standard non-containerized application.

## Troubleshooting

### Database Connection Issues

- Ensure PostgreSQL is running: `pg_isready` or `psql -U postgres -c "SELECT 1"`
- Verify database exists: `psql -U postgres -l | grep aitradexdb`
- Check user permissions: `psql -U postgres -c "\du aitradex_user"`

### Port Conflicts

- Backend default port: `8080` (override with `SERVER_PORT` environment variable)
- Frontend default port: `4200` (override with `PORT` environment variable or `ng serve --port <port>`)

### Liquibase Migration Issues

- Check logs for migration errors
- Verify database user has CREATE/ALTER privileges
- Reset database if needed: `psql -U postgres -f db/scripts/05-reset-database.sql`
