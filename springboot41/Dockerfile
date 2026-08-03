# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Cache dependencies separately from source so `docker compose build` doesn't
# re-download the internet every time a .java file changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /build/target/springboot41-0.0.1-SNAPSHOT.jar app.jar

# 8080: HTTP (web/restclient samples), 9090: Spring gRPC server default port
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
