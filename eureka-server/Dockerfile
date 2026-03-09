FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/eureka-server-*.jar app.jar

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --quiet --tries=1 --spider http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
