# Aitradex Container Build & Run Guide

This document summarizes how to build, package, and run the Aitradex services with Docker and Docker Compose.

## Images
- **Backend**: `aitradex-service` (Spring Boot 3, Java 21)  
- **Frontend**: `aitradex-ui` (Angular)  
- **Database**: `postgres:16-alpine`

## Build
```bash
# From repo root
cd aitradex
docker build -t aitradex-service -f aitradex-service/Dockerfile .
docker build -t aitradex-ui -f aitradex-ui/Dockerfile ./aitradex-ui
```

## Run (Compose)
```bash
cd aitradex
docker-compose up --build
# API:    http://localhost:8080/api
# UI:     http://localhost:4200/
# DB:     localhost:5432 (user/pass: aitradex/aitradex)
```

## Package artifacts
- Backend JAR is produced by `mvn -pl aitradex-service -am package -DskipTests`.
- Angular build artifacts are emitted to `aitradex-ui/dist/aitradex-ui`.

## Environment toggles
- `SPRING_PROFILES_ACTIVE=prod` (default in Compose)
- `SPRING_DATASOURCE_URL`/`POSTGRES_USER`/`POSTGRES_PASSWORD` override DB connection.
- `JAVA_OPTS` can be supplied to the backend container for memory and GC tuning.
- Swagger UI available at `http://localhost:8080/api/swagger-ui.html` when service is up.

## Notes
- The backend Dockerfile uses multi-stage Maven build. Ensure network access to Maven Central when building the image.
- Compose includes a named volume `pgdata` for database persistence between runs.
