# ---- Build stage (root context required for parent POM) ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY notification-service/pom.xml notification-service/pom.xml
COPY notification-service/src notification-service/src
RUN apk add --no-cache maven \
  && mvn -f notification-service/pom.xml package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/notification-service/target/notification-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
