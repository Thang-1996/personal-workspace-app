FROM eclipse-temurin:21-jre
WORKDIR /application
COPY api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
