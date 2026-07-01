FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY iam_service/pom.xml iam_service/
COPY condominio_service/pom.xml condominio_service/
COPY portaria-service/pom.xml portaria-service/
COPY notificacao-service/pom.xml notificacao-service/
COPY auditoria_service/pom.xml auditoria_service/
COPY infra/eureka-server/pom.xml infra/eureka-server/
COPY infra/api-gateway/pom.xml infra/api-gateway/
COPY infra/condoplus-observability-starter/pom.xml infra/condoplus-observability-starter/
RUN mvn dependency:go-offline -pl iam_service -am -q --no-transfer-progress 2>/dev/null || true
COPY infra/condoplus-observability-starter/src infra/condoplus-observability-starter/src
COPY iam_service/src iam_service/src
RUN mvn package -pl iam_service -am -DskipTests -q --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/iam_service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
