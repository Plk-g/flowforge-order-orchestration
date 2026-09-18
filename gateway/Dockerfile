FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY platform/pom.xml platform/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY platform platform
COPY order-service order-service
COPY gateway gateway
COPY payment-service payment-service
RUN mvn -q -pl gateway -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/gateway/target/gateway-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
