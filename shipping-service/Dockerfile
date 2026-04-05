# ---- Build stage (root context required for parent POM) ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
# Copy root POM first so child can resolve parent
COPY pom.xml .
COPY shipping-service/pom.xml shipping-service/pom.xml
COPY shipping-service/src shipping-service/src
RUN apk add --no-cache maven \
  && mvn -f shipping-service/pom.xml package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/shipping-service/target/shipping-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
