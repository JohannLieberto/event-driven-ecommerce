# ---- Build stage (root context required for parent POM) ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY payment-service/pom.xml payment-service/pom.xml
COPY payment-service/src payment-service/src
RUN apk add --no-cache maven \
  && mvn -f payment-service/pom.xml package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/payment-service/target/payment-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
