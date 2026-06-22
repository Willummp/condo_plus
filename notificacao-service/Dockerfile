FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY notificacao-service/pom.xml notificacao-service/
COPY iam_service/pom.xml iam_service/
COPY condominio_service/pom.xml condominio_service/
COPY portaria-service/pom.xml portaria-service/
COPY auditoria_service/pom.xml auditoria_service/
COPY infra/eureka-server/pom.xml infra/eureka-server/
COPY infra/api-gateway/pom.xml infra/api-gateway/
RUN mvn dependency:go-offline -pl notificacao-service -am -q --no-transfer-progress 2>/dev/null || true
COPY notificacao-service/src notificacao-service/src
RUN mvn package -pl notificacao-service -am -DskipTests -q --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/notificacao-service/target/notificacao-service-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
