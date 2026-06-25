# Condo+ — Sistema de Gestão de Condomínios Residenciais

Sistema distribuído baseado em microsserviços para modernizar a gestão de condomínios residenciais. Resolve problemas como conflitos de reservas em áreas comuns, rastreabilidade de encomendas, controle de acesso físico, aplicação de multas e comunicação entre síndico e moradores.

---

## Equipe

| Integrante | Microsserviço | Responsabilidade |
|---|---|---|
| Lucas Ferreira | `condominio-service` | Core de estrutura, convivência e testes de integração |
| Caio | `iam-service` | Autenticação, credenciais e autorização JWT |
| Nicholas | `portaria-service` | Controle de acesso físico, visitantes e encomendas |
| Rodolpho | `notificacao-service` | Notificações reativas e preferências de canal |
| Fernando | `auditoria-service` | Histórico de operações e detecção de anomalias |

---

## Arquitetura Geral

```
                        [ Cliente ]
                             │
                             ▼  :8080
                      [ API Gateway ]
                      Valida JWT + injeta
                      X-User-Id/Email/Roles
                             │
        ┌────────────────────┼────────────────────┐
        ▼ :8081              ▼ :8082              ▼ :8083
   [ iam-service ]  [ condominio-service ] [ portaria-service ]
   Spring MVC       Spring MVC + JDBC       Spring MVC + JPA
   JWT (jjwt)       Kafka producer          Kafka producer + Redis
        │                    │                    │
        └────────────────────┴────────────────────┤
                             │
                   ┌─────────┴──────────┐
                   ▼ :8084              ▼ :8085
          [ notificacao-service ] [ auditoria-service ]
          WebFlux + R2DBC         Spring MVC + MongoDB
          Kafka consumer          Kafka consumer

Infraestrutura:
  PostgreSQL :5432  |  MongoDB :27017  |  Kafka :9092  |  Redis :6379
  Eureka :8761  (Service Discovery)
```

Todos os serviços se registram no **Eureka** ao iniciar. A comunicação entre eles usa o nome lógico (ex: `lb://iam-service`) — sem IPs fixos. O **API Gateway** é o único ponto de entrada externo.

---

## Tecnologias por Serviço

### infra/api-gateway (porta 8080)

| Tecnologia | Por que foi usada |
|---|---|
| Spring Cloud Gateway (WebFlux/Netty) | Roteamento reativo não-bloqueante; suporte nativo a filtros globais e por rota |
| `JwtAuthenticationFilter` (customizado) | Valida assinatura HMAC-SHA256 e expiração do token **antes** de encaminhar para qualquer serviço; injeta `X-User-Id`, `X-User-Email` e `X-User-Roles` nos headers downstream |
| `CorrelationGatewayFilter` | Gera ou propaga `X-Correlation-ID` em todas as requisições para rastreabilidade entre logs dos serviços |
| Spring Cloud LoadBalancer | Balanceia chamadas para os nomes lógicos do Eureka (`lb://nome-servico`) |
| StripPrefix | Remove prefixo da rota antes de encaminhar: `StripPrefix=1` para a maioria dos serviços, `StripPrefix=2` para IAM (cujo controller não tem prefixo no path interno) |

**Tabela de roteamento:**

| Rota externa (Gateway) | Encaminha para | StripPrefix |
|---|---|---|
| `/api/condominio/**` | `lb://condominio-service` | 1 |
| `/api/portaria/**` | `lb://portaria-service` | 1 |
| `/api/auditoria/**` | `lb://auditoria-service` | 1 |
| `/api/iam/**` | `lb://iam-service` | 2 |
| `/api/notificacoes/**` | `lb://notificacao-service` | 1 |

---

### iam-service (porta 8081)

| Tecnologia | Por que foi usada |
|---|---|
| Spring Security | Proteção das rotas de gerenciamento de credenciais (`ADMIN`/`SINDICO`) via `@PreAuthorize` |
| jjwt (io.jsonwebtoken) | Geração e verificação de tokens JWT com HMAC-SHA256; separação clara entre `accessToken` (curta duração) e `refreshToken` |
| Spring Data JPA + Hibernate | Mapeamento ORM das entidades `Credencial` e `RefreshToken` com relacionamento `@ManyToMany` para roles |
| Flyway | Versionamento do schema do banco (`V1`, `V2__seed_admin.sql`) — seed do admin inicial aplicado automaticamente |
| Spring Kafka | Publica eventos de criação/bloqueio de credencial para outros serviços |
| Testcontainers (PostgreSQL) | Testes de integração sobem um PostgreSQL real em container Docker, garantindo que as queries e migrations funcionam de verdade |

---

### condominio-service (porta 8082)

| Tecnologia | Por que foi usada |
|---|---|
| Spring Data JDBC | DDD com `Unidade` como Aggregate Root — Spring Data JDBC respeita os limites de agregado (sem lazy loading acidental), evitando consultas N+1 e mantendo a consistência do domínio |
| Spring Security (header-based) | Consome `X-User-Id`/`X-User-Roles` injetados pelo Gateway para montar o `SecurityContext`; sem redeclarar JWTs — sem dupla validação |
| Resilience4j (Circuit Breaker + TimeLimiter) | Chamada síncrona ao `iam-service` para criar credenciais; abre o circuito se > 50% das últimas 10 chamadas falharem ou estourarem 3 s de timeout; evita cascata de falhas |
| Spring Kafka | Publica eventos de domínio (`MULTA_APLICADA`, `COMUNICADO_PUBLICADO`, `RESERVA_CONFIRMADA`, `ENCOMENDA_RECEBIDA`) consumidos por `notificacao-service` e `auditoria-service` |
| Flyway | Migrations versionadas com schema isolado `condominio` |
| Micrometer + Prometheus | Métricas customizadas de negócio: `condoplus.multas.aplicadas`, `condoplus.comunicados.publicados`, `condoplus.reservas.confirmadas` |

---

### portaria-service (porta 8083)

| Tecnologia | Por que foi usada |
|---|---|
| Spring Data JPA | Persistência de `Visitante`, `Encomenda` e `RegistroAcesso` com relacionamentos mais complexos entre entidades |
| Spring Data Redis | TTL automático por tipo de encomenda: `CURTO_PRAZO` = 2 h (iFood), `MEDIO_PRAZO` = 7 dias (Amazon), `LONGO_PRAZO` = 30 dias (Shopee) — Redis garante expiração sem job agendado |
| Spring Kafka | Publica `ENCOMENDA_RECEBIDA` ao registrar encomenda e eventos de acesso |
| Resilience4j | Tolerância a falhas em chamadas a outros serviços |
| Testcontainers (PostgreSQL + Redis) | ITs sobem banco e cache reais para validar a lógica de TTL e queries |

---

### notificacao-service (porta 8084)

| Tecnologia | Por que foi usada |
|---|---|
| Spring WebFlux + Netty | Serviço totalmente reativo (não-bloqueante) — adequado para um serviço de notificações que escuta eventos e entrega para múltiplos canais de forma concorrente sem bloquear threads |
| Spring Data R2DBC | Driver reativo para PostgreSQL; sem JDBC bloqueante no caminho de dados — mantém o modelo reativo ponta-a-ponta |
| reactor-kafka | Consumo reativo de tópicos Kafka; integração nativa com `Flux`/`Mono` do Project Reactor |
| Resilience4j Reactor | Variante reativa do Resilience4j — `CircuitBreaker` e `Retry` compatíveis com `Mono`/`Flux` |
| Spring Security Reactive | Versão reativa do Spring Security para WebFlux, consome headers `X-User-*` injetados pelo Gateway |
| Flyway (JDBC datasource separado) | Flyway não suporta R2DBC diretamente; usa um `DataSource` JDBC apenas para rodar as migrations na inicialização, enquanto o runtime usa R2DBC |

---

### auditoria-service (porta 8085)

| Tecnologia | Por que foi usada |
|---|---|
| Spring Data MongoDB | Documentos de auditoria têm schema flexível (payload varia por tipo de evento); MongoDB é ideal para armazenar JSON livre sem migrations de coluna |
| Spring Kafka | Consome todos os eventos do ecossistema e persiste no MongoDB como trilha de auditoria imutável |
| Flapdoodle Embed MongoDB | MongoDB embutido em memória para testes — não exige Docker no runner de CI |
| spring-kafka-test (`@EmbeddedKafka`) | Kafka embutido para ITs sem infraestrutura externa |

---

### infra/eureka-server (porta 8761)

Spring Cloud Netflix Eureka Server puro. Todos os microsserviços registram seu endereço ao iniciar. O Gateway e os clientes Feign/WebClient resolvem `lb://nome` consultando o Eureka — nenhum IP é hardcoded.

---

## Segurança — Arquitetura JWT (TP3)

O modelo adotado é **Autenticação Centralizada na Borda**:

```
[ Cliente ]
    │  Bearer Token no header Authorization
    ▼
[ API Gateway ]
    ├── Token inválido/ausente → 401 (interrompe aqui)
    └── Token válido → injeta X-User-Id, X-User-Email, X-User-Roles
              │
              ▼
[ Microsserviço ]
    └── Lê headers, monta SecurityContext, aplica @PreAuthorize
```

- O **IAM** é o único serviço que **emite** tokens.
- O **Gateway** é o único que **valida** a assinatura — os microsserviços downstream confiam nos headers injetados.
- `accessToken`: curta duração (15 min por padrão).
- `refreshToken`: longa duração, endpoint `/api/iam/auth/refresh`.

### Endpoints Públicos (sem autenticação)

| Rota (Gateway) | Serviço | Descrição |
|---|---|---|
| `POST /api/iam/auth/login` | iam-service | Autenticação — retorna access + refresh token |
| `POST /api/iam/auth/refresh` | iam-service | Renova o accessToken |
| `GET /actuator/health` | Todos | Health check |

### Endpoints Protegidos — Roles Exigidas

| Rota (Gateway) | Método | Role mínima |
|---|---|---|
| `/api/condominio/unidades` | POST | SINDICO, ADMIN |
| `/api/condominio/unidades/{id}/vinculacoes` | POST | SINDICO, ADMIN |
| `/api/condominio/pessoas` | POST | SINDICO, ADMIN |
| `/api/condominio/multas` | POST | ADMIN, SINDICO |
| `/api/condominio/comunicados` | POST | ADMIN, SINDICO |
| `/api/condominio/**` | GET | Qualquer autenticado |
| `/api/iam/credenciais` | POST | ADMIN, SINDICO |
| `/api/iam/credenciais/{id}/status` | PATCH | ADMIN |
| `/api/portaria/**` | POST/PATCH | PORTEIRO |
| `/api/notificacoes/minhas` | GET | Qualquer autenticado |
| `/api/notificacoes/{id}/retry` | POST | ADMIN, SINDICO |
| `/api/auditoria/**` | GET | ADMIN, SINDICO |

---

## Como Rodar o Projeto (do Zero)

### Pré-requisitos

- Docker Desktop instalado e rodando
- Maven 3.9+ e JDK 21

### 1. Gerar as imagens Docker

Execute na raiz do projeto:

```bash
# Compilar todos os módulos e gerar os JARs
mvn package -DskipTests

# Construir cada imagem
docker build -t condoplus/eureka-server      infra/eureka-server
docker build -t condoplus/api-gateway        infra/api-gateway
docker build -t condoplus/iam-service        iam_service
docker build -t condoplus/condominio-service condominio_service
docker build -t condoplus/portaria-service   portaria-service
docker build -t condoplus/notificacao-service notificacao-service
docker build -t condoplus/auditoria-service  auditoria_service
```

### 2. Subir tudo com Docker Compose

```bash
docker compose up -d
```

Isso sobe os 12 containers na ordem correta:

| Container | Porta | Descrição |
|---|---|---|
| `condoplus-postgres` | 5432 | PostgreSQL 16 (todos os schemas de negócio) |
| `condoplus-postgres-init` | — | Inicializa os schemas/databases |
| `condoplus-mongo` | 27017 | MongoDB 7 (auditoria) |
| `condoplus-kafka` | 9092 | Apache Kafka 3.7 |
| `condoplus-redis` | 6379 | Redis 7 (TTL de encomendas) |
| `condoplus-eureka` | 8761 | Eureka Server |
| `condoplus-iam` | 8081 | IAM Service |
| `condoplus-condominio` | 8082 | Condomínio Service |
| `condoplus-portaria` | 8083 | Portaria Service |
| `condoplus-notificacao` | 8084 | Notificação Service |
| `condoplus-auditoria` | 8085 | Auditoria Service |
| `condoplus-gateway` | 8080 | API Gateway (único ponto de entrada) |

### 3. Verificar se está rodando

```bash
# Verificar status de todos os containers
docker compose ps
```

**Dashboard do Eureka:** abra `http://localhost:8761` no navegador. Você verá todos os serviços registrados com status `UP`. Aguarde ~30 segundos após o `docker compose up` para todos se registrarem.

### 4. Primeiro acesso

O Flyway aplica `V2__seed_admin.sql` automaticamente ao subir o IAM. Credencial inicial:

```
email: admin@condoplus.com
senha: Admin@1234
```

```bash
# Login
curl -s -X POST http://localhost:8080/api/iam/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@condoplus.com","senha":"Admin@1234"}'

# Usar o token retornado
curl -H "Authorization: Bearer <ACCESS_TOKEN>" \
  http://localhost:8080/api/condominio/unidades
```

### 5. Coleção Postman

Importe o arquivo `postman/condoplus.postman_collection.json`. Ele contém todos os endpoints com bodies corretos, variáveis de coleção e um script que salva o token automaticamente após o login.

### Parar o ambiente

```bash
docker compose down          # para containers (volumes preservados)
docker compose down -v       # para containers e apaga todos os dados
```

### 6. Frontend

O frontend é um único arquivo HTML (`condo_plus_app.html`) que funciona como SPA. Basta abrir no navegador — não precisa de servidor nem build:

1. Com todos os containers rodando, abra o arquivo `condo_plus_app.html` diretamente no navegador (duplo clique ou `File > Open`).
2. Faça login com as credenciais do admin (`admin@condoplus.com` / `Admin@1234`) ou de qualquer usuário cadastrado.

O frontend se comunica com o gateway em `http://localhost:8080`. Funcionalidades por perfil:

| Perfil | Acesso |
|---|---|
| ADMIN | Tudo — pessoas, unidades, multas, comunicados, áreas, portaria, auditoria |
| SINDICO | Gestão operacional — multas, comunicados, reservas, portaria |
| PORTEIRO | Visitantes, encomendas e registro de entrada de moradores |
| MORADOR | Dashboard pessoal, reservas de áreas comuns e notificações |
| FUNCIONARIO | Dashboard pessoal e notificações |

O token JWT é renovado automaticamente via refresh token — a sessão não expira enquanto o navegador estiver aberto.

---

## Como Rodar os Testes

Cada serviço separa testes unitários (Surefire) de testes de integração (Failsafe) por convenção de nomenclatura:
- `*Test.java` → unitários, sem infraestrutura externa
- `*IT.java` → integração, sobem banco/Kafka/Redis via Testcontainers ou embarcado

### condominio-service

```bash
# Unitários apenas
mvn test -pl condominio_service

# Unitários + integração (Testcontainers sobe PostgreSQL real + Kafka embutido)
mvn verify -pl condominio_service
```

| Classe | Tipo | O que testa |
|---|---|---|
| `EscopoDerivacaoServiceTest` | Unitário | Lógica de derivação de escopos de vinculação |
| `PessoaServiceTest` | Unitário | Criação de pessoa com mock do IamClient |
| `UnidadeRepositoryIT` | Integração | Queries do repositório contra PostgreSQL real (`@DataJdbcTest`) |
| `CondominioKafkaIT` | Integração | Publicação de eventos Kafka |

### iam-service

```bash
# Unitários
mvn test -pl iam_service

# Integração (Testcontainers sobe PostgreSQL, valida fluxo login → JWT completo)
mvn failsafe:integration-test failsafe:verify -pl iam_service

# Tudo de uma vez
mvn verify -pl iam_service
```

| Classe | Tipo | O que testa |
|---|---|---|
| `JwtServiceTest` | Unitário | Geração e validação de tokens |
| `AutenticacaoServiceTest` | Unitário | Fluxo de login com mock do repositório |
| `AutenticacaoControllerTest` | Unitário | Camada HTTP |
| `AutenticacaoIntegrationIT` | Integração | Login + refresh contra banco real |

### portaria-service

```bash
mvn verify -pl portaria-service
```

| Classe | Tipo | O que testa |
|---|---|---|
| `VisitanteServiceTest` | Unitário | Regras de autorização de visitantes |
| `VisitanteRepositoryIT` | Integração | Queries contra PostgreSQL real |
| `EncomendaServiceIT` | Integração | Lógica de TTL e registro de encomendas |

### notificacao-service

```bash
# Unitários
mvn test -pl notificacao-service

# Integração (@DataR2dbcTest sobe PostgreSQL via Testcontainers)
mvn verify -pl notificacao-service
```

| Classe | Tipo | O que testa |
|---|---|---|
| `NotificacaoServiceTest` | Unitário | Processamento de eventos Kafka |
| `PreferenciaControllerTest` | Unitário | Endpoints de preferências com mock de segurança |
| `PreferenciaRepositoryIT` | Integração | Operações reativas contra PostgreSQL real |

### auditoria-service

```bash
mvn verify -f auditoria_service/pom.xml
```

| Classe | Tipo | O que testa |
|---|---|---|
| `AuditoriaKafkaIntegracaoTest` | Integração | Consome evento Kafka e persiste no MongoDB embutido (Flapdoodle + `@EmbeddedKafka`) |

> **Atenção:** exige Docker para Testcontainers quando presente. O Flapdoodle (MongoDB embutido) não funciona no Ubuntu 24 — por isso o CI usa `ubuntu-22.04`.

### api-gateway e eureka-server

```bash
mvn test -pl infra/api-gateway
mvn test -pl infra/eureka-server
```

Apenas verificam que o contexto Spring carrega corretamente.

---

## GitHub Actions — CI por Serviço

Cada módulo tem seu próprio workflow em `.github/workflows/`. Os workflows são disparados **apenas quando arquivos do módulo ou o `pom.xml` raiz mudam** — um commit que toca só o `portaria-service` não roda o CI do `condominio-service`.

| Workflow | Arquivo | Runner | O que roda |
|---|---|---|---|
| API Gateway | `api-gateway.yml` | ubuntu-latest | `mvn test -pl infra/api-gateway` |
| Eureka Server | `eureka-server.yml` | ubuntu-latest | `mvn test -pl infra/eureka-server` |
| IAM Service | `iam-service.yml` | ubuntu-latest | Unit tests + Integration tests (Failsafe) em steps separados |
| Condomínio | `condominio-service.yml` | ubuntu-latest | `mvn test -pl condominio_service` |
| Portaria | `portaria-service.yml` | ubuntu-latest | `mvn verify -pl portaria-service` |
| Notificação | `notificacao-service.yml` | ubuntu-latest | Build → unit tests → integration tests em etapas separadas |
| Auditoria | `auditoria-service-ci.yml` | **ubuntu-22.04** | `mvn verify -f auditoria_service/pom.xml` (Flapdoodle + MongoDB 7 não é compatível com Ubuntu 24) |

Todos os workflows usam:
- **JDK 21 Temurin** via `actions/setup-java@v4`
- **Cache Maven** — dependências não são rebaixadas do zero a cada execução
- Filtro de **paths por módulo** — evita execuções desnecessárias

---

## Estrutura de Pastas

```
condo_plus/
│
├── pom.xml                               # POM raiz — parent de todos os módulos
├── docker-compose.yml                    # Orquestração completa (12 containers)
│
├── postman/
│   └── condoplus.postman_collection.json # Coleção com todos os endpoints e variables
│
├── docs/
│   ├── TP1.md / TP2.md / TP3.md         # Enunciados dos trabalhos práticos
│   └── docs_integrantes/                 # Documentação individual por integrante
│
├── infra/
│   ├── eureka-server/                    # Spring Cloud Eureka Server (:8761)
│   │   ├── src/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   └── api-gateway/                      # Spring Cloud Gateway (:8080)
│       ├── src/
│       ├── Dockerfile
│       └── pom.xml
│
├── iam_service/                          # Autenticação e credenciais (:8081)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── condominio_service/                   # Core: unidades, pessoas, multas, reservas (:8082)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── portaria-service/                     # Controle de acesso, visitantes, encomendas (:8083)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── notificacao-service/                  # Notificações reativas via Kafka (:8084)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── auditoria_service/                    # Trilha de auditoria em MongoDB (:8085)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
└── .github/
    └── workflows/                        # 7 workflows de CI (um por módulo)
        ├── api-gateway.yml
        ├── eureka-server.yml
        ├── iam-service.yml
        ├── condominio-service.yml
        ├── portaria-service.yml
        ├── notificacao-service.yml
        └── auditoria-service-ci.yml
```

---

## Estrutura Interna dos Serviços

### iam-service

```
iam_service/src/main/java/com/condoplus/iam/
├── config/          # SecurityConfig, JwtConfig
├── controller/      # AutenticacaoController (/auth/login, /auth/refresh)
│                    # CredencialController (/credenciais)
├── domain/          # Credencial, RefreshToken
│                    # TipoRole: ADMIN, SINDICO, PORTEIRO, MORADOR, FUNCIONARIO
│                    # StatusCredencial: ATIVO, BLOQUEADO, BLOQUEADO_TEMPORARIAMENTE
├── dto/             # LoginRequest, TokenResponse, CriarCredencialRequest
├── repository/      # CredencialRepository, RefreshTokenRepository
├── security/        # HeaderAuthenticationFilter (lê X-User-* do Gateway)
└── service/         # AutenticacaoService, JwtService, CredencialService
```

### condominio-service

```
condominio_service/src/main/java/com/condoplus/condominio/
├── config/          # SecurityConfig, KafkaConfig, WebClientConfig
├── estrutura/
│   ├── client/      # IamClient (Resilience4j + WebClient → iam-service)
│   ├── controller/  # UnidadeController, PessoaController, VeiculoController
│   │                # FuncionarioController, AreaComumController
│   ├── domain/      # Unidade (Aggregate Root), Pessoa, Veiculo, Funcionario, Vinculacao
│   │                # CargoFuncionario: PORTEIRO, JARDINEIRO, LIMPEZA, ADMINISTRATIVO
│   │                # TipoVinculacao: PROPRIETARIO, RESIDENTE, PROPRIETARIO_RESIDENTE
│   ├── dto/
│   ├── kafka/       # EventoPublisher
│   ├── repository/
│   └── service/     # UnidadeService, PessoaService, EscopoDerivacaoService
└── convivencia/
    ├── controller/  # MultaController, ComunicadoController, ReservaController
    ├── domain/      # Multa, Comunicado, Reserva
    │                # CategoriaMulta: CONVIVENCIA, ESTRUTURAL
    │                # PublicoAlvo: TODOS, PROPRIETARIOS, RESIDENTES, BLOCO_ESPECIFICO
    ├── dto/
    ├── repository/
    └── service/
```

### portaria-service

```
portaria-service/src/main/java/com/condoplus/portaria_service/
├── config/          # SecurityConfig, RedisConfig, KafkaConfig
├── controller/      # EncomendaController, VisitanteController, AcessoController
├── model/
│   ├── entity/      # Encomenda, Visitante, RegistroAcesso, AutorizacaoVisitante
│   └── enums/       # TipoEncomenda: CURTO_PRAZO, MEDIO_PRAZO, LONGO_PRAZO
│                    # TipoVisitante: SOCIAL, VISITANTE, PRESTADOR
│                    # TipoPessoaAcesso: MORADOR, VISITANTE, FUNCIONARIO, PRESTADOR
├── repository/
└── service/         # EncomendaService, VisitanteService, AcessoService
```

### notificacao-service

```
notificacao-service/src/main/java/com/condoplus/notificacao/
├── config/          # SecurityConfig (reactive), KafkaConfig, R2dbcConfig
├── controller/      # NotificacaoController (/notificacoes/minhas, /{id}/retry)
│                    # PreferenciaController (/notificacoes/preferencias)
├── domain/          # Notificacao, Preferencia
│                    # TipoEvento: COMUNICADO_PUBLICADO, MULTA_APLICADA,
│                    #             ENCOMENDA_RECEBIDA, RESERVA_CONFIRMADA
│                    # Canal: EMAIL, PUSH, WHATSAPP, IN_APP
├── dto/
├── kafka/           # NotificacaoConsumer (reactor-kafka)
├── repository/      # NotificacaoRepository, PreferenciaRepository (ambos R2DBC)
└── service/         # NotificacaoService (Mono/Flux), PreferenciaService
```

### auditoria-service

```
auditoria_service/src/main/java/com/condoplus/auditoria/
├── config/          # SecurityConfig, KafkaConfig, MongoConfig
├── controller/      # AuditoriaController (/registros, /anomalias)
├── domain/          # RegistroAuditoria (Document MongoDB), Anomalia
│                    # StatusAnomalia: ABERTA, RECONHECIDA, RESOLVIDA
├── dto/
├── kafka/           # AuditoriaConsumer (escuta todos os tópicos de eventos)
├── repository/      # RegistroAuditoriaRepository, AnomaliaRepository
└── service/         # AuditoriaService
```

### api-gateway

```
infra/api-gateway/src/main/java/com/condoplus/gateway/
├── config/          # GatewayConfig (rotas por application.yml)
├── filter/          # JwtAuthenticationFilter (valida JWT + injeta headers X-User-*)
│                    # CorrelationGatewayFilter (X-Correlation-ID)
└── security/        # JwtUtil (decodificação e validação HMAC-SHA256)
```

---

## Fluxo de Cadastro (ponta-a-ponta)

```
1. POST /api/iam/auth/login
   └── Retorna accessToken (ADMIN ou SINDICO)

2. POST /api/condominio/pessoas               [Authorization: Bearer <token>]
   └── Cria Pessoa no condomínio
   └── Chama iam-service (via Circuit Breaker) para criar Credencial

3. POST /api/condominio/unidades
   └── Cria Unidade

4. POST /api/condominio/unidades/{id}/vinculacoes
   └── Vincula Pessoa à Unidade como PROPRIETARIO / RESIDENTE / PROPRIETARIO_RESIDENTE

5. POST /api/portaria/encomendas              [Authorization: Bearer <token PORTEIRO>]
   └── Registra encomenda com TTL no Redis
   └── Publica ENCOMENDA_RECEBIDA no Kafka
        ├── notificacao-service: entrega notificação no canal preferido do morador
        └── auditoria-service: grava trilha imutável no MongoDB
```
