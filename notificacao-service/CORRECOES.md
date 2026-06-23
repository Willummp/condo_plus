# Correções aplicadas — notificacao-service

## 1. WebClientConfig — `@LoadBalanced` faltando

**Arquivo:** `src/main/java/.../config/WebClientConfig.java`

O bean `WebClient.Builder` não tinha a anotação `@LoadBalanced`. Sem ela, o Spring Cloud
LoadBalancer não intercepta chamadas com esquema `lb://` e o Eureka é ignorado.

```java
@Bean
@LoadBalanced
public WebClient.Builder webClientBuilder() { ... }
```

---

## 2. CondominioWebClient — URL errada (`http://` → `lb://`)

**Arquivo:** `src/main/java/.../client/CondominioWebClient.java`

As chamadas ao condominio-service usavam `http://condominio-service/...`, que depende
de DNS do Docker Compose. Fora desse ambiente (Eureka, load balancer), a chamada falha.
Corrigido para `lb://condominio-service/...`.

---

## 3. ResolverDestinatariosService — loop de redelivery no Kafka

**Arquivo:** `src/main/java/.../service/ResolverDestinatariosService.java`

O caminho `COMUNICADO_PUBLICADO` (broadcast para todos os moradores) não tinha
`onErrorResume`. Se a chamada ao condominio-service falhasse, o `ack.acknowledge()`
nunca era invocado e o Kafka reenviava a mensagem indefinidamente.

---

## 4. application.yml — bloco JWT Keycloak indevido

**Arquivo:** `src/main/resources/application.yml`

Havia uma configuração de `spring.security.oauth2.resourceserver.jwt` apontando para
Keycloak. O notificacao-service não usa Keycloak — a autenticação é feita via headers
`X-User-Id` e `X-User-Roles` injetados pelo API Gateway. O bloco foi removido.

---

## 5. Dockerfile — build incompatível com docker-compose raiz

**Arquivo:** `Dockerfile`

O Dockerfile original só funcionava buildado de dentro da pasta do serviço. O
docker-compose raiz usa `context: .` (raiz do projeto), exigindo um build multi-módulo
que copie os `pom.xml` de todos os módulos antes de compilar.

---

## 6. pom.xml — dependências Resilience4j faltando

**Arquivo:** `pom.xml`

O `application.yml` configurava um circuit breaker (`resilience4j.circuitbreaker`) mas
as dependências `resilience4j-spring-boot3` e `resilience4j-reactor` não estavam
declaradas no `pom.xml`. O circuit breaker era silenciosamente ignorado.

---

## 7. API Gateway — StripPrefix=1 (deveria ser 2)

**Arquivo:** `infra/api-gateway/src/main/resources/application.yml`

A rota do notificacao-service foi alterada de `StripPrefix=2` para `StripPrefix=1`,
quebrando o roteamento via gateway.

- Com `StripPrefix=1`: `/api/notificacao/notificacoes/minhas` → `/notificacao/notificacoes/minhas` → **404**
- Com `StripPrefix=2`: `/api/notificacao/notificacoes/minhas` → `/notificacoes/minhas` → **correto**

Os controllers estão mapeados em `/notificacoes/`, sem prefixo `/notificacao/`.

---

## 8. pom.xml — versão explícita sem SNAPSHOT

**Arquivo:** `pom.xml`

O módulo declarava `<version>1.0.0</version>` explicitamente, sobrescrevendo o parent
que define `1.0.0-SNAPSHOT`. Todos os outros serviços herdam a versão do parent sem
redeclará-la. A linha foi removida para padronizar.

O Dockerfile também foi atualizado para copiar o jar com o nome correto
(`notificacao-service-1.0.0-SNAPSHOT.jar`).

---

## 9. compose isolado — credenciais não injetadas

**Arquivo:** `compose/docker-compose.yml`

O serviço `notificacao-app` no compose isolado não injetava `NOTIFICACAO_DB_USER` nem
`NOTIFICACAO_DB_PASSWORD`. O serviço usava os defaults do `application.yml`
(`notificacao_user` / `notificacao_password`), mas o postgres do compose usa
`condominio_user` / `condominio_password`. A conexão falharia ao subir a aplicação
em container via compose isolado.

```yaml
environment:
  - NOTIFICACAO_DB_USER=condominio_user
  - NOTIFICACAO_DB_PASSWORD=condominio_password
```

**Impacto:** apenas o compose isolado. O docker-compose raiz já injetava corretamente.
