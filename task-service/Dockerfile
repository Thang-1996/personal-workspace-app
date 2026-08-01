FROM eclipse-temurin:21-jre
WORKDIR /application
COPY task-service/target/task-service-0.0.1-SNAPSHOT.jar application.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "application.jar"]
