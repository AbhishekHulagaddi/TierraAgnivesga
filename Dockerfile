FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY TutionManagemnet_updated/pom.xml .
RUN mvn dependency:go-offline

COPY TutionManagemnet_updated/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
