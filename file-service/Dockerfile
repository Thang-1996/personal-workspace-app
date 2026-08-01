FROM eclipse-temurin:21-jre
WORKDIR /application
COPY file-service/target/file-service-0.0.1-SNAPSHOT.jar application.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","application.jar"]
