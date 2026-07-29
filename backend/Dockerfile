# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM first and resolve dependencies in their own layer: dependencies rarely
# change between builds, so this layer stays cached even when only source files change.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
# Tests already run separately (mvn test / mvn verify -Pintegration-tests) — skipping them
# here avoids needing Docker-in-Docker for Testcontainers during the image build itself.
RUN mvn -q package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/messaging-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
