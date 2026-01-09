# syntax=docker/dockerfile:1.6

#
# Build stage
#
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy only pom files first for better layer caching
COPY pom.xml .
COPY aitradex-service/pom.xml aitradex-service/pom.xml
COPY aitradex-ui/pom.xml aitradex-ui/pom.xml

# Download dependencies (will be cached unless pom changes)
RUN mvn -pl aitradex-service -am dependency:go-offline

# Copy source
COPY aitradex-service/src aitradex-service/src

# Build the service (tests can be skipped in container builds)
RUN mvn -pl aitradex-service -am package -DskipTests

#
# Runtime stage
#
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS=""
WORKDIR /app

COPY --from=builder /workspace/aitradex-service/target/aitradex-service-*.jar /app/aitradex-service.jar
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/aitradex-service.jar"]
