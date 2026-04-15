FROM node:20-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml ./
COPY src ./src
COPY --from=frontend /build/src/main/resources/static ./src/main/resources/static/

RUN mvn -B -DskipTests -Dskip.npm=true package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
