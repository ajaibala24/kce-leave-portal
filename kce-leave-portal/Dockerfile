# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render gives the port in $PORT; falls back to 8080 locally
CMD ["sh", "-c", "java -Xmx300m -jar app.jar --server.port=${PORT:-8080}"]
