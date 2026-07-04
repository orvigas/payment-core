# Stage 1 Build
FROM maven:3-amazoncorretto-23-alpine AS builder

WORKDIR /app

COPY pom.xml .

# The cache mount persists ~/.m2 across separate `docker build` runs, not just within
# one build's layer cache. Without it, any change to pom.xml or src invalidates this
# layer and every dependency gets re-downloaded from scratch on the next build.
# -B (batch mode) turns off Maven's animated progress bars, which render as garbled
# noise through BuildKit's log streaming.
RUN --mount=type=cache,target=/root/.m2 \
  mvn -B dependency:resolve

COPY src src

# Same cache mount as above, so packaging reuses whatever dependency:resolve already
# pulled down instead of re-fetching it.
RUN --mount=type=cache,target=/root/.m2 \
  mvn -B package -DskipTests

# Stage 2 Runtime
FROM amazoncorretto:23-alpine

WORKDIR /app

COPY --from=builder /app/target/payment-core-1.0.0.jar app.jar

EXPOSE 8080 5005

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]