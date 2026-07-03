# ---------- build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /usr/src/app

# Copy pom.xml first (so dependencies can be cached)
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

# Copy source code and resources
COPY src ./src
COPY src/main/resources/application.yml.example src/main/resources/application.yml

# Build the JAR
RUN mvn -B package -DskipTests && cp target/*.jar app.jar

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
# Create a non-root user for safety
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
WORKDIR /app

# Copy only the built JAR from builder stage
COPY --from=builder /usr/src/app/app.jar /app/app.jar

# Allow custom JVM options at runtime
ENV JAVA_OPTS=""
# Render/Heroku will inject PORT — app must bind to it
EXPOSE 8080

USER appuser

# Start app, use PORT if provided (default 10000)
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
