# Observabilidade — Condo+

Documentação da stack de observabilidade do Condo+: o que foi implementado,
como funciona, e por que cada tecnologia foi escolhida.

---

## Sumário

- [Contexto](#contexto)
- [Visão geral da stack](#visão-geral-da-stack)
- [Decisões de tecnologia](#decisões-de-tecnologia)
- [O que foi implementado](#o-que-foi-implementado)
- [Como funciona na prática](#como-funciona-na-prática)
- [Endpoints e acessos](#endpoints-e-acessos)
- [Como validar](#como-validar)

---

## Contexto

O Condo+ é um sistema de gestão condominial composto por 5 microsserviços
de negócio (`iam`, `condominio`, `portaria`, `notificacao`, `auditoria`)
mais 2 componentes de infraestrutura (`eureka-server` como service
discovery e `api-gateway` como entrypoint).

**Problema que a observabilidade resolve.** Em uma arquitetura distribuída,
uma única requisição do usuário atravessa vários serviços via HTTP
síncrono e mensageria Kafka. Sem tracing correlacionado, métricas
agregadas e logs com identificadores comuns, investigar um incidente
vira arqueologia — pula-se de log em log tentando cruzar timestamps.

**Três pilares implementados:**

- **Tracing distribuído** — cada request ganha um `traceId` que se
  propaga entre serviços, permitindo reconstruir a jornada completa
  no Zipkin.
- **Métricas** — coletadas por serviço (JVM, HTTP, Kafka, circuit
  breakers) e centralizadas no Prometheus para consulta e dashboards
  no Grafana.
- **Logs correlacionados** — cada linha de log inclui o `traceId`,
  permitindo cruzar logs com traces sem depender de timestamps.

---

## Visão geral da stack

```
┌─────────────────────────────────────────────────────────────────┐
│                        Microsserviços                            │
│  iam, condominio, portaria, notificacao, auditoria, gateway     │
│                                                                  │
│  Cada serviço tem:                                              │
│  - Micrometer Observation API (instrumentação)                  │
│  - OpenTelemetry SDK (bridge de tracing)                        │
│  - Actuator: /actuator/prometheus, /actuator/health              │
│  - Logs com traceId/spanId no MDC                               │
└────────────┬────────────────────────────┬───────────────────────┘
             │                            │
       spans (Zipkin V2)          scrape /actuator/prometheus
             │                            │
             ▼                            ▼
     ┌───────────────┐            ┌──────────────┐
     │    Zipkin     │            │  Prometheus  │
     │ localhost:9411│            │ localhost:9090│
     └───────────────┘            └──────┬───────┘
                                         │
                                    consulta
                                         │
                                         ▼
                                  ┌──────────────┐
                                  │   Grafana    │
                                  │localhost:3000│
                                  └──────────────┘
                                         │
                                         │ descobre alvos via
                                         │ eureka_sd_configs
                                         ▼
                                  ┌──────────────┐
                                  │Eureka Server │
                                  │localhost:8761│
                                  └──────────────┘
```

---

## Decisões de tecnologia

### 1. Bridge de tracing: OpenTelemetry

**Contexto.** Micrometer Tracing (successor do Spring Cloud Sleuth no
Spring Boot 3.x) suporta dois bridges: Brave e OpenTelemetry.

**Escolha.** OpenTelemetry (`micrometer-tracing-bridge-otel`).

**Por quê.** Brave é mais simples e vai direto para Zipkin, mas trava a
stack em um único backend. OpenTelemetry é o padrão de mercado atual,
suporta exportadores para praticamente qualquer backend (Zipkin, Jaeger,
Tempo, Datadog, New Relic), e é o caminho evolutivo natural.

### 2. Backend de tracing: Zipkin

**Escolha.** Zipkin com exporter direto (`opentelemetry-exporter-zipkin`).

**Por quê.** UI leve, roda em uma única imagem sem dependências externas,
e o exporter é estável. Perfeito para o escopo do projeto: aprendizado
concreto de tracing distribuído sem overhead de operar um Collector
intermediário.

### 3. Centralização de configuração: starter interno

**Contexto.** Cada microsserviço precisava das mesmas configurações de
observabilidade (endpoints, tags, patterns). Três caminhos possíveis:

- **(a)** Spring Cloud Config Server
- **(b)** `application-common.yml` em módulo compartilhado
- **(c)** Starter interno com auto-configuration

**Escolha.** Combinação de (c) + (b): starter interno que empacota um
`application-observability.yml` e uma auto-configuration.

**Por quê.** Config Server adiciona mais um serviço para manter e um
repositório Git separado — desproporcional para o escopo. Só arquivo
YAML compartilhado seria simples demais e não permitiria beans
customizados no futuro. O starter interno dá o meio-termo ideal:
dependência única por serviço, permite `@Bean`s e `@Conditional` para
extensões, e é o padrão idiomático do ecossistema Spring Boot.

**Como o serviço consome:**

```xml
<!-- pom.xml do serviço -->
<dependency>
    <groupId>com.condoplus</groupId>
    <artifactId>condoplus-observability-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

```yaml
# application.yml do serviço
spring:
  application:
    name: iam-service
  config:
    import: classpath:application-observability.yml
```

### 4. Métricas: Prometheus + Grafana

**Escolha.** Prometheus para coleta/armazenamento; Grafana para
visualização.

**Por quê.** Padrão de fato do ecossistema cloud-native, integração
nativa com Spring Boot Actuator via `micrometer-registry-prometheus`,
modelo pull compatível com service discovery, e PromQL como query
language poderosa. Grafana é o par natural — provisionamento de
dashboards como código, datasource pré-configurado, e comunidade
com dashboards prontos de alta qualidade.

### 5. Service Discovery para Prometheus: `eureka_sd_configs` nativo

**Contexto.** O Condo+ usa Netflix Eureka para service discovery entre
microsserviços. O Prometheus precisa saber onde raspar métricas.

**Escolha.** `eureka_sd_configs` nativo do Prometheus.

**Por quê.** O Prometheus tem suporte oficial a Eureka. Sem adapter,
sem sidecar, sem container extra. Basta apontar para o Eureka Server e
o Prometheus consulta a REST API dele periodicamente, mantendo a lista
de alvos sincronizada. Subir um novo microsserviço = alvo aparece
automaticamente em ~15s.

### 6. Dashboards: 1 customizado + 2 da comunidade

**Escolha.**

- **Condo+ - Visão Geral** (customizado, versionado no repo): painel
  único com status de todos os serviços, throughput, latência p95,
  taxa de erro, uso de heap e Kafka lag. Reflete o vocabulário do
  projeto.
- **JVM Micrometer** (Grafana.com id `4701`): visão detalhada por
  instância de JVM.
- **Spring Boot 3 Statistics** (Grafana.com id `19004`): breakdown
  por endpoint HTTP, HikariCP, etc.

**Por quê.** Dashboards da comunidade cobrem 80% das necessidades
técnicas genéricas — reinventá-los seria desperdício. O dashboard
customizado serve como "home" do projeto, com nomenclatura específica
dos serviços do Condo+.

### 7. Logs: `application.yml`, não `logback-spring.xml`

**Escolha.** Configurar logs via propriedades `logging.*` no
`application.yml`, sem `logback-spring.xml` customizado.

**Por quê.** A necessidade é apenas patternizar console incluindo
`traceId`/`spanId`. Isso cabe em uma propriedade. Migrar para
`logback-spring.xml` só compensa quando precisar de appenders
customizados (JSON estruturado, async logging, appenders para
sistemas externos) — complexidade que não se justifica no escopo atual.

### 8. Regras de alerta no Prometheus, sem Alertmanager

**Escolha.** Definir 5 regras de alerta no Prometheus, mas sem subir
Alertmanager.

**Por quê.** As regras são avaliadas continuamente pelo Prometheus e
visíveis em `http://localhost:9090/alerts`. Isso é suficiente para
demonstrar detecção de anomalias e para uso em desenvolvimento.
Alertmanager (roteamento + notificação por Slack/email/webhook) exige
configuração de credenciais e canais externos que fogem ao escopo.

---

## O que foi implementado

### Módulo `condoplus-observability-starter`

Starter interno que centraliza toda a configuração de observabilidade.
Vive em `infra/condoplus-observability-starter/`.

Estrutura:

```
infra/condoplus-observability-starter/
├── pom.xml
└── src/main/
    ├── java/com/condoplus/observability/
    │   ├── ObservabilityAutoConfiguration.java
    │   └── CondoplusObservabilityProperties.java
    └── resources/
        ├── META-INF/spring/
        │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
        └── application-observability.yml
```

O que o starter traz:

- Dependências transitivas: `spring-boot-starter-actuator`,
  `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-zipkin`,
  `micrometer-registry-prometheus`.
- `application-observability.yml` com defaults compartilhados
  (endpoints do Actuator, tags de métricas, sampling de tracing,
  endpoint do Zipkin, pattern de log correlacionado, observation
  habilitada em Kafka).
- Auto-configuration ativada pelo Spring Boot 3 via
  `AutoConfiguration.imports`.

### Ajustes em cada microsserviço

Cada um dos 6 serviços recebeu:

1. Dependência do starter no `pom.xml`.
2. `spring.config.import: classpath:application-observability.yml` no
   `application.yml`.
3. Remoção de propriedades duplicadas de observabilidade que agora
   vivem no starter.
4. Cópia do starter no `Dockerfile` (para o build multi-módulo funcionar
   dentro do container).
5. Variáveis de ambiente no `docker-compose.yml`
   (`ZIPKIN_ENDPOINT: http://zipkin:9411/api/v2/spans`).

Nos serviços com Spring Security, foi necessário liberar
`/actuator/**` para permitir o scrape do Prometheus:

```java
// MVC (iam, condominio, portaria)
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**").permitAll()
    .anyRequest().authenticated()
)

// WebFlux (notificacao)
.authorizeExchange(exchanges -> exchanges
    .pathMatchers("/actuator/**").permitAll()
    .anyExchange().authenticated()
)
```

### Infraestrutura de observabilidade

Três serviços adicionados ao `docker-compose.yml`:

- **Zipkin** (`openzipkin/zipkin:3`, porta 9411) — armazenamento
  in-memory de traces.
- **Prometheus** (`prom/prometheus:v2.55.0`, porta 9090) — coleta e
  armazenamento de métricas com retenção de 7 dias.
- **Grafana** (`grafana/grafana:11.3.0`, porta 3000) — visualização
  com datasource e dashboards provisionados.

Configurações organizadas em `infra/observability/`:

```
infra/observability/
├── prometheus/
│   ├── prometheus.yml               ← descoberta via eureka_sd_configs
│   └── rules/
│       └── condoplus-alerts.yml     ← 5 regras de alerta
└── grafana/
    ├── provisioning/
    │   ├── datasources/prometheus.yml
    │   └── dashboards/condoplus.yml
    └── dashboards/
        └── condoplus-overview.json  ← dashboard customizado
```

### Regras de alerta

5 alertas cobrindo cenários críticos:

| Alerta                | Condição                                       | Severity |
|-----------------------|------------------------------------------------|----------|
| ServicoFora           | `up == 0` por 1 min                            | critical |
| TaxaErroHttp5xxAlta   | > 5% de respostas 5xx em 5 min                 | warning  |
| LatenciaP95Alta       | p95 HTTP > 2s por 5 min                        | warning  |
| HeapAlto              | uso de heap > 85% por 5 min                    | warning  |
| ConsumerLagAlto       | Kafka consumer lag > 1000 msgs por 3 min       | warning  |

---

## Como funciona na prática

### Fluxo de um trace distribuído

1. Cliente faz `POST /pessoas` no Gateway (porta 8080).
2. Gateway gera um `traceId` novo (128 bits, formato W3C).
3. Gateway repassa para o `condominio-service`, injetando o header
   `traceparent` na requisição HTTP.
4. `condominio-service` continua o trace (novo `spanId`, mesmo `traceId`)
   e chama o `iam-service` via WebClient. Header propagado novamente.
5. `iam-service` cria mais um span, executa a lógica, publica evento
   Kafka. O `traceId` também vai no header da mensagem Kafka.
6. Consumer do `condominio-service` continua o trace no listener
   Kafka (Micrometer instrumenta automaticamente com
   `observation-enabled: true`).
7. Cada serviço envia seus spans para o Zipkin via HTTP POST em
   `/api/v2/spans`.
8. No Zipkin UI, buscando pelo `traceId`, você vê a árvore completa
   de spans com timing de cada um.

### Fluxo das métricas

1. Cada serviço expõe `/actuator/prometheus` — endpoint em formato
   texto com todas as métricas do Micrometer (JVM, HTTP, Kafka,
   circuit breaker, custom).
2. Prometheus consulta o Eureka a cada 15s para descobrir quais
   serviços estão registrados como `UP`.
3. Para cada serviço descoberto, Prometheus faz scrape do endpoint
   a cada 10s.
4. Métricas armazenadas com labels `application`, `instance`, `tier`.
5. Grafana consulta o Prometheus via PromQL e renderiza os painéis
   dos dashboards.
6. Prometheus avalia as regras de alerta a cada 15s. Alertas em
   estado `PENDING` (condição verdadeira mas ainda não atingiu `for:`)
   ou `FIRING` (condição sustentada) ficam visíveis em `/alerts`.

### Correlação log ↔ trace

Cada request HTTP passa por um filtro que coloca `traceId` e `spanId`
no MDC do Logback. O pattern de log injetado pelo starter usa
`%X{traceId:-}` e `%X{spanId:-}`, então cada linha sai com o formato:

```
2026-06-30 22:42:22.316 [http-nio-8082-exec-3] INFO  c.c.c.e.service.PessoaService [CID=abc,trace=1d199ea268e4e76dde210e617dfbb7eb,span=fa0a4ff74f851d6a] - Pessoa cadastrada com sucesso...
```

Copiando o valor de `trace=` e colando no Zipkin, você vê o trace
correspondente àquela linha específica.

---

## Endpoints e acessos

| Serviço      | URL                              | Uso                                    |
|--------------|----------------------------------|----------------------------------------|
| Zipkin UI    | http://localhost:9411            | Buscar traces por ID ou por serviço    |
| Prometheus   | http://localhost:9090            | Query ad-hoc, targets, regras          |
| Grafana      | http://localhost:3000            | Dashboards (login admin/admin)         |
| Eureka       | http://localhost:8761            | Ver serviços registrados               |
| Actuator     | http://localhost:80XX/actuator   | Health, métricas de um serviço         |

Depois do Grafana subir, importar os dashboards da comunidade via UI:

1. Menu **Dashboards** → **New** → **Import**.
2. IDs para importar: **`4701`** (JVM Micrometer) e **`19004`**
   (Spring Boot 3).
3. Datasource: Prometheus.

O dashboard customizado **Condo+ - Visão Geral** é provisionado
automaticamente, sem ação manual.

---

## Como validar

### Tracing

Dispare uma request que atravesse pelo menos 2 serviços (ex.: cadastrar
uma pessoa via Gateway). Nos logs de qualquer serviço envolvido,
procure uma linha com `trace=<hex>`. Cole esse valor em:

`http://localhost:9411` → **Find a Trace** → cole no campo Trace ID →
**Run Query**

O trace deve aparecer com spans conectados dos serviços envolvidos.

### Métricas

`http://localhost:9090/targets` — o job `condoplus-services` deve
listar todos os serviços registrados no Eureka como `UP`.

Uma query rápida para confirmar dados chegando:

```
sum by (application) (rate(http_server_requests_seconds_count[1m]))
```

### Regras de alerta

`http://localhost:9090/rules` — deve listar os 5 alertas com estado
atual. `http://localhost:9090/alerts` mostra os que estão em
`PENDING`/`FIRING`.

### Dashboard

`http://localhost:3000` (admin/admin) → pasta **Condo+** →
**Condo+ - Visão Geral**. Todos os painéis devem ter dado (se estiverem
vazios, dispare algumas requests pelo Gateway para gerar carga).

---

## Referências

- [Spring Boot Actuator - Tracing](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)
- [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/)
- [Prometheus - Eureka SD](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#eureka_sd_config)
- [Grafana Dashboards](https://grafana.com/grafana/dashboards/)
