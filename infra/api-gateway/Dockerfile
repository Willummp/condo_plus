FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY infra/api-gateway/pom.xml infra/api-gateway/
COPY iam_service/pom.xml iam_service/
COPY condominio_service/pom.xml condominio_service/
COPY portaria-service/pom.xml portaria-service/
COPY notificacao-service/pom.xml notificacao-service/
COPY auditoria_service/pom.xml auditoria_service/
COPY infra/eureka-server/pom.xml infra/eureka-server/
RUN mvn dependency:go-offline -pl infra/api-gateway -am -q --no-transfer-progress 2>/dev/null || true
COPY infra/api-gateway/src infra/api-gateway/src
RUN mvn package -pl infra/api-gateway -am -DskipTests -q --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/infra/api-gateway/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
