# Multi-stage Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml mvnw* ./
COPY src ./src
RUN ./mvnw -q -DskipTests package || mvn -DskipTests package

FROM eclipse-temurin:17-jre-alpine
ARG JAR_FILE=target/*.jar
COPY --from=build /workspace/${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

