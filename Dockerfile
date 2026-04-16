FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY . .

WORKDIR /app/TutionManagemnet_updated
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY --from=build /app/TutionManagemnet_updated/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
