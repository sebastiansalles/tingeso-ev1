# Runtime image: only a Java 21 JRE, no Maven nor source code
FROM eclipse-temurin:21-jre

WORKDIR /app

# The jar is built before (locally or in the pipeline) with: mvn clean package
COPY target/siga-backend.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]