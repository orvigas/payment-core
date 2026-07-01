# Stage 1: Build
FROM maven:3-amazoncorretto-23-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM amazoncorretto:23-alpine
WORKDIR /app
COPY --from=builder /app/target/payment-core-1.0.0.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher

ENTRYPOINT ["java", "-jar", "app.jar"]