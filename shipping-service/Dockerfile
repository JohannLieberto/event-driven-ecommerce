FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/shipping-service-*.jar app.jar

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --quiet --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
