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
- **Docker + Docker Compose** (optional, for containerized runs)

## Build / Install Dependencies

### Backend (Spring Boot)

```bash
cd aitradex
mvn -pl aitradex-service -am package
```

If you want to skip tests during packaging:

```bash
mvn -pl aitradex-service -am package -DskipTests
```

### Frontend (Angular)

```bash
cd aitradex/aitradex-ui
npm install
npm run build
```

Build artifacts are emitted to `aitradex-ui/dist/aitradex-ui`.

### Container Images (optional)

```bash
cd aitradex
docker build -t aitradex-service -f aitradex-service/Dockerfile .
docker build -t aitradex-ui -f aitradex-ui/Dockerfile ./aitradex-ui
```

## Tests

### Backend

```bash
cd aitradex
mvn -pl aitradex-service test
```

> Tests use Testcontainers and require Docker to be running.

### Frontend

```bash
cd aitradex/aitradex-ui
npm test
```

Optional linting:

```bash
npm run lint
```

## Run the Application

### Option 1: Run everything with Docker Compose (recommended)

```bash
cd aitradex
docker-compose up --build
```

Services and ports:

- **UI**: `http://localhost:4200/`
- **API**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **Database**: `localhost:5432` (user/pass: `aitradex` / `aitradex`)

### Option 2: Run backend and frontend locally (dev)

#### Start PostgreSQL

Using Docker:

```bash
docker run --name aitradex-postgres -e POSTGRES_DB=aitradex -e POSTGRES_USER=aitradex -e POSTGRES_PASSWORD=aitradex -p 5432:5432 -d postgres:16-alpine
```

#### Run the backend

```bash
cd aitradex
mvn -pl aitradex-service spring-boot:run
```

The backend listens on `http://localhost:8080` and exposes API routes under `/api`.

#### Run the frontend

```bash
cd aitradex/aitradex-ui
npm install
npm start
```

The Angular dev server runs at `http://localhost:4200/` and proxies API calls to `http://localhost:8080/api` via `environment.ts`.

### Option 3: Run only the backend

```bash
cd aitradex
mvn -pl aitradex-service spring-boot:run
```

### Option 4: Run only the frontend

```bash
cd aitradex/aitradex-ui
npm install
npm start
```

> Note: The UI expects the API at `http://localhost:8080/api` (see `aitradex-ui/src/environments/environment.ts`).

## Access the Application

- **UI**: `http://localhost:4200/`
- **API base URL**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **Health check**: `http://localhost:8080/actuator/health`

## Environment Configuration

The backend uses environment variables for configuration. Common ones include:

- `SPRING_DATASOURCE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: database connection

See `aitradex/CONTAINERS.md` and `aitradex-service/src/main/resources/application.yml` for more defaults and profiles.
