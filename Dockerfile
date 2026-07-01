FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY target/gestor-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]