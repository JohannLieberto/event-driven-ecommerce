# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY eureka-server/src eureka-server/src
RUN apk add --no-cache maven \
  && mvn -f eureka-server/pom.xml package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/eureka-server/target/eureka-server-*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
