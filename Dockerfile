# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first so source-only changes do not refetch the world.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the application.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

# The ECS task definition / ALB target groups use ContainerPort 80, so the app
# binds :80. Port 80 is privileged, so the container runs as root (the default)
# to bind it. Override SERVER_PORT to change the listen port.
ENV SERVER_PORT=80
# Default local-disk storage location (only used when STORAGE_DRIVER=local;
# in production IMAGE_BUCKET is set and the app uploads to S3 instead).
ENV STORAGE_LOCAL_DIR=/data/uploads
RUN mkdir -p /data/uploads

EXPOSE 80

# The ALB blue/green target groups health-check GET / (must return 200-399).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
