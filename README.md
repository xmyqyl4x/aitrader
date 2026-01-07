# Aitrader / Aitradex

This repository contains the Spring Boot backend (`aitradex-service`) and the Angular UI (`aitradex-ui`).

## Prerequisites

- Java 21 (for the Spring Boot service)
- Maven 3.9+
- Node.js 18+ and npm (for the Angular UI)
- Docker (optional, for containerized runs)

## Build

### Full workspace build

```bash
mvn -f aitradex/pom.xml clean package
```

### Backend only

```bash
mvn -f aitradex/aitradex-service/pom.xml clean package
```

### UI only

```bash
cd aitradex/aitradex-ui
npm install
npm run build
```

## Test

### Run all backend tests

```bash
mvn -f aitradex/aitradex-service/pom.xml test
```

### Run a single test class

```bash
mvn -f aitradex/aitradex-service/pom.xml -Dtest=RiskControllerTest test
```

### UI tests

```bash
cd aitradex/aitradex-ui
npm test
```

## Run

### Option 1: Docker Compose (backend + UI + database)

```bash
cd aitradex
./mvnw -q -DskipTests package
npm --prefix aitradex-ui install
npm --prefix aitradex-ui run build

# Build and run containers

docker compose up --build
```

Access:

- UI: http://localhost:4200
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/api/swagger-ui.html

### Option 2: Local backend + local database

1. Start PostgreSQL locally (or use the Docker service below):

```bash
docker run --name aitradex-postgres -e POSTGRES_DB=aitradex -e POSTGRES_USER=aitradex -e POSTGRES_PASSWORD=aitradex -p 5432:5432 -d postgres:16-alpine
```

2. Run the Spring Boot service:

```bash
mvn -f aitradex/aitradex-service/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Access:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/api/swagger-ui.html

### Option 3: Local UI (Angular dev server)

```bash
cd aitradex/aitradex-ui
npm install
npm start
```

Access:

- UI: http://localhost:4200

### Option 4: Run backend and UI separately (no Docker)

1. Start the backend (see Option 2).
2. Start the UI dev server (see Option 3).
3. Configure a token in the UI (Configuration page) if your API endpoints require JWT auth.

## Configuration

- Backend configuration lives in `aitradex/aitradex-service/src/main/resources/application.yml`.
- Docker Compose configuration is in `aitradex/docker-compose.yml`.
