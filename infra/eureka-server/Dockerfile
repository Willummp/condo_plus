FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY infra/eureka-server/pom.xml infra/eureka-server/
COPY iam_service/pom.xml iam_service/
COPY condominio_service/pom.xml condominio_service/
COPY portaria-service/pom.xml portaria-service/
COPY notificacao-service/pom.xml notificacao-service/
COPY auditoria_service/pom.xml auditoria_service/
COPY infra/api-gateway/pom.xml infra/api-gateway/
RUN mvn dependency:go-offline -pl infra/eureka-server -am -q --no-transfer-progress 2>/dev/null || true
COPY infra/eureka-server/src infra/eureka-server/src
RUN mvn package -pl infra/eureka-server -am -DskipTests -q --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/infra/eureka-server/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
