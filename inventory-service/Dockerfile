# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY inventory-service/src inventory-service/src
RUN apk add --no-cache maven \
  && mvn -f inventory-service/pom.xml package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/inventory-service/target/inventory-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
