# Stage 1: Build the executable JAR
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and Kotlin DSL build scripts first to cache dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# Copy source code and package the JAR (excluding plain JARs)
COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy the executable Spring Boot JAR
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Xmx350m", "-XX:+UseSerialGC", "-jar", "app.jar"]
