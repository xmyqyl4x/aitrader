# Build and Run Instructions

## ⚠️ Important: Directory Requirements

**You MUST run build and run commands from the individual project directories, NOT from the root directory.**

- **Backend commands**: Must be run from `C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service`
- **Frontend commands**: Must be run from `C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui`

## Quick Start

### Prerequisites

- PostgreSQL running on `localhost:5432`
- Database `aitradexdb` and user `aitradex_user` created (see `db/scripts/`)

### 1. Build Backend

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn clean install -DskipTests
```

Expected output:
- JAR file created: `target/aitradex-service-0.0.1-SNAPSHOT.jar`
- Build status: `BUILD SUCCESS`

### 2. Build Frontend

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm install --legacy-peer-deps
npm run build
```

Expected output:
- Build artifacts in: `dist/aitradex-ui/`
- Build status: `√ Compiled successfully`

### 3. Run Backend

**Terminal 1 - Backend:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Wait for:
- `Tomcat started on port 8080`
- `Started AitradexApplication`

Access:
- API: http://localhost:8080/api
- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/api/swagger-ui.html

### 4. Run Frontend

**Terminal 2 - Frontend:**

```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm start
```

Wait for:
- `Angular Live Development Server is listening on localhost:4200`
- `√ Compiled successfully`

Access:
- UI: http://localhost:4200

## Directory Structure

```
C:\dev2025\java-projects\devspaces\aitrader\aitradex\
├── aitradex-service\          ← Run backend commands here
│   ├── pom.xml
│   ├── src\
│   └── target\                ← Build output here
│       └── aitradex-service-0.0.1-SNAPSHOT.jar
├── aitradex-ui\               ← Run frontend commands here
│   ├── package.json
│   ├── src\
│   └── dist\                  ← Build output here
│       └── aitradex-ui\
└── db\                        ← Database scripts
    └── scripts\
```

## Build Verification

### Backend Build Success

✅ **Compilation successful**
```bash
[INFO] BUILD SUCCESS
[INFO] Total time:  X.XXX s
```

✅ **JAR created**
```bash
[INFO] Building jar: ...\target\aitradex-service-0.0.1-SNAPSHOT.jar
```

### Frontend Build Success

✅ **Dependencies installed**
```bash
up to date, audited XXX packages
```

✅ **Build completed**
```bash
√ Browser application bundle generation complete.
√ Copying assets complete.
√ Index html generation complete.
Build at: ... - Hash: ... - Time: XXXXms
```

## Run Verification

### Backend Running

✅ **Liquibase migrations**
```
[INFO] liquibase.util : Total change sets: X
[INFO] liquibase.command : Command execution complete
```

✅ **Tomcat started**
```
[INFO] o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat started on port 8080
```

✅ **Application started**
```
[INFO] com.myqyl.aitradex.AitradexApplication : Started AitradexApplication in X.XXX seconds
```

✅ **Health check**
```bash
curl http://localhost:8080/actuator/health
# Returns: {"status":"UP"}
```

### Frontend Running

✅ **Development server**
```
** Angular Live Development Server is listening on localhost:4200 **
√ Compiled successfully.
```

✅ **Browser access**
- Open http://localhost:4200
- Should load the Angular application

## Troubleshooting

### Backend Won't Start from Root

❌ **Error:**
```
[ERROR] Failed to execute goal ...spring-boot:run... on project aitradex: 
Unable to find a suitable main class
```

✅ **Solution:**
Run from `aitradex-service` directory, not root:
```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend Dependency Conflicts

❌ **Error:**
```
npm error peer @angular/core@"^18.0.0" from @fortawesome/angular-fontawesome@0.15.0
npm error Conflicting peer dependency
```

✅ **Solution:**
Use `--legacy-peer-deps`:
```bash
cd C:\dev2025\java-projects\devspaces\aitrader\aitradex\aitradex-ui
npm install --legacy-peer-deps
```

### Database Connection Issues

❌ **Error:**
```
[ERROR] Connection to localhost:5432 refused
```

✅ **Solution:**
1. Verify PostgreSQL is running: `pg_isready`
2. Verify database exists: `psql -U postgres -l | grep aitradexdb`
3. Check credentials in `application.yml`

## Common Commands

### Backend (from `aitradex-service`)

```bash
# Clean and compile
mvn clean compile

# Build (skip tests)
mvn clean package -DskipTests

# Run application
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
mvn test

# Run JAR
java -jar target/aitradex-service-0.0.1-SNAPSHOT.jar
```

### Frontend (from `aitradex-ui`)

```bash
# Install dependencies
npm install --legacy-peer-deps

# Development server
npm start

# Production build
npm run build

# Run tests
npm test

# Lint
npm run lint
```

## Summary

✅ **Build Backend**: `cd aitradex-service && mvn clean install`
✅ **Build Frontend**: `cd aitradex-ui && npm install --legacy-peer-deps && npm run build`
✅ **Run Backend**: `cd aitradex-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
✅ **Run Frontend**: `cd aitradex-ui && npm start`

**Remember:** Always run commands from the appropriate project directory!
