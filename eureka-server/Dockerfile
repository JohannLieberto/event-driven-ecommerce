FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache wget

WORKDIR /app

COPY target/eureka-server-*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
