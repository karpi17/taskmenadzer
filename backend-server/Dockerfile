# Stage 1: Build
FROM gradle:8.4-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Stage 2: Run
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
