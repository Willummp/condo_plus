# 📚 Documentação de Estudos — TP1 (Fundação, Integração e Resiliência)

Este documento foi elaborado para consolidar os conceitos teóricos e práticos abordados no **Trabalho Prático 1 (TP1)** do projeto **Condo+**, com referências diretas de onde cada conceito está aplicado no código.

---

## 1. Arquitetura de Microsserviços e Infraestrutura

A fundação do Condo+ no TP1 é composta por:
1. **API Gateway (Porta 8080):** Ponto único de entrada para todas as requisições. Responsável pelo roteamento, segurança de borda (validação JWT) e filtros globais.
2. **Eureka Server (Porta 8761):** Servidor de registro de serviços (*Service Registry*). Os microsserviços se registram dinamicamente nele ao iniciarem, permitindo que se comuniquem por nomes lógicos (ex: `iam-service`, `condominio-service`) ao invés de endereços IP e portas estáticas.
3. **Bancos de Dados Isolados:** Cada serviço possui sua própria instância de banco de dados PostgreSQL (com schemas lógicos separados).

📍 **Onde encontrar no código:**
- **Eureka Server:** [application.yml](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/infra/eureka-server/src/main/resources/application.yml)
- **API Gateway:** Roteamentos em [application.yml](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/infra/api-gateway/src/main/resources/application.yml)

---

## 2. Spring Data JDBC (Domain-Driven Design Puro)

O **Spring Data JDBC** foi escolhido para o `condominio-service` por incentivar um design muito mais aderente aos princípios de **DDD (Domain-Driven Design)**. Diferente do JPA (Hibernate), o JDBC corta as "mágicas" de *Dirty Checking* e *Lazy Loading*.

📍 **Onde encontrar no código:**
- **Mapeamento de Coleções no Agregado:** Em [Unidade.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/estrutura/domain/Unidade.java), a anotação `@MappedCollection(idColumn = "unidade_id")` define que *Vinculações* pertencem exclusivamente ao Agregado *Unidade*.
- **Salvamento Explícito:** Em métodos dos Controllers ou Services (como no `UnidadeController`), é estritamente necessário chamar `unidadeRepository.save(unidade)` para que qualquer alteração vá para o banco.

---

## 3. Resilience4j (Tolerância a Falhas)

Utilizado para proteger as chamadas síncronas entre microsserviços contra falhas em cascata. Usamos **Circuit Breaker** (abre o circuito se houver muitos erros) e **Time Limiter** (aborta se demorar demais).

📍 **Onde encontrar no código:**
- **Configuração de Limites:** No arquivo [application.yml](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/resources/application.yml) (procure por `resilience4j.circuitbreaker` e `resilience4j.timelimiter`).
- **Aplicação no Cliente HTTP:** Na classe [IamClient.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/estrutura/client/IamClient.java), veja as anotações `@CircuitBreaker(name = "iamService")` e `@TimeLimiter(name = "iamService")` e o método de `fallback`.

---

## 4. Segurança Stateless (Baseada em Headers)

O API Gateway valida o token na borda e repassa a identidade do usuário via Headers HTTP (`X-User-Id` e `X-User-Roles`). O microsserviço confia nessa informação.

📍 **Onde encontrar no código:**
- **Filtro de Segurança:** Em [SecurityConfig.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/config/SecurityConfig.java), observe o `Filter` customizado que lê `request.getHeader("X-User-Id")` e constrói o `UsernamePasswordAuthenticationToken` do Spring Security dinamicamente.

---

## 5. Nível de Isolamento `SERIALIZABLE`

Evita conflitos de concorrência garantindo que duas reservas de área comum no mesmo horário nunca sejam salvas ao mesmo tempo.

📍 **Onde encontrar no código:**
- **Reserva Service:** Em [ReservaService.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/convivencia/service/ReservaService.java), veja a anotação `@Transactional(isolation = Isolation.SERIALIZABLE)` no método `criar(...)`. Se o PostgreSQL detectar concorrência, ele abortará uma das transações (SSI).

---

## 6. Testcontainers

Permite rodar testes de integração contra um contêiner real de PostgreSQL gerado dinamicamente via Docker, em vez de bancos limitados em memória (como H2).

📍 **Onde encontrar no código:**
- **Suíte de Testes:** Em [UnidadeRepositoryIT.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/test/java/com/condoplus/condominio/estrutura/repository/UnidadeRepositoryIT.java), veja as anotações `@Testcontainers` e `@Container static PostgreSQLContainer<?> postgres`.
