# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/doc-qa-rag-*.jar app.jar
EXPOSE 8080
# PORT env (default 8080) is honoured by the app itself.
ENTRYPOINT ["java", "-jar", "app.jar"]
