# Attic - Container Artifacts

This directory contains container-related artifacts that have been archived as the project has been refactored to run as a non-containerized application.

## Contents

- `docker-compose.yml` - Docker Compose configuration (archived)
- `CONTAINERS.md` - Container build and run documentation (archived)
- `.dockerignore` - Docker ignore file (archived)
- `aitradex-service-Dockerfile` - Backend Dockerfile (archived)
- `aitradex-ui-Dockerfile` - Frontend Dockerfile (archived)
- `aitradex-ui-nginx.conf` - Nginx configuration for frontend container (archived)

## Current Status

The application now runs as a standard non-containerized application:

- **Backend**: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- **Frontend**: `npm start`
- **Database**: Local PostgreSQL installation

See the main `README.md` for current build and run instructions.

## Historical Reference

These files are kept for reference in case containerization is needed in the future or for deployment purposes.
