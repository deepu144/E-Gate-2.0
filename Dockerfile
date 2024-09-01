FROM maven:3.8.5-openjdk-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests
FROM openjdk:21-slim
WORKDIR /app
COPY --from=build /app/target/E-Gate-2-0.0.1-SNAPSHOT.jar egate.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "egate.jar"]
