# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/pom.xml
RUN mvn -f eureka-server/pom.xml dependency:go-offline -B
COPY eureka-server/src eureka-server/src
RUN mvn -f eureka-server/pom.xml package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/eureka-server/target/eureka-server-*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
