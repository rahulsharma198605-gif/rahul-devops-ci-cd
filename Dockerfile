# Use official lightweight Java runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/webapp-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
