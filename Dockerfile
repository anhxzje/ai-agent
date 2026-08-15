# Stage 1: Build JAR application using Maven
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy maven wrapper & pom.xml first for dependency caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code and build executable jar
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Minimal Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Expose port 8080
EXPOSE 8080

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Environment defaults
ENV SPRING_PROFILES_ACTIVE=prod

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
