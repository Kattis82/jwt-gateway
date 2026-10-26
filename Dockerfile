FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9003

ENTRYPOINT ["java", "-jar", "app.jar"]