FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/shipping-service-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java","-jar","/app/app.jar"]
