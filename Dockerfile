FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/E-Gate-2-0.0.1-SNAPSHOT.jar egate.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "egate.jar"]