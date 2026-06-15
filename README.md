# 🏢 Condo+ — Sistema de Gestão de Condomínios Residenciais

### Divisão de Responsabilidades por Microsserviço:
| Aluno | Microsserviço sob Responsabilidade | Papel / Responsabilidade Adicional |
|---|---|---|
| **Lucas Ferreira** | `condominio-service` | Desenvolvimento do Core de Estrutura, Convivência e Testes de Integração |
| **Fernando Telheiros** | `iam-service` | Gerenciamento de Credenciais de Acesso e Autenticação (Spring Security) |
| **Integrante 3** | `portaria-service` | Controle de Acesso Físico, Visitantes e Recepção de Encomendas |
| **Integrante 4** | `notificacao-service` | Envio de Alertas e Notificações (E-mail, SMS, Push) |
| **Integrante 5** | `auditoria-service` | Histórico e Auditoria de Operações Críticas do Condomínio |

---

## 📝 Descrição do Projeto
O **Condo+** é um sistema distribuído para modernizar e simplificar a gestão de condomínios residenciais. O sistema resolve problemas como:
* Falha de comunicação na portaria sobre quem tem acesso à unidade.
* Conflitos de reservas em áreas comuns (churrasqueiras, salões de festas).
* Rastreabilidade falha no recebimento e entrega de encomendas.
* Distribuição ineficiente de multas estruturais e de convivência.

---

## 🗺️ Arquitetura do Sistema
A arquitetura do Condo+ é construída sobre microsserviços desacoplados e resilientes:

```
                          [ Cliente ]
                               │
                               ▼ (Porta 8080)
                       [ API Gateway ] ◄──── Confirma Rotas
                               │
       ┌───────────────┬───────┴───────┬───────────────┐
       ▼               ▼               ▼               ▼
[ condominio ]      [ iam ]       [ portaria ]  [ notificacao ]
 (Porta 8082)    (Porta 8081)     (Porta 8083)    (Porta 8084)
       │               │               │               │
       ▼               ▼               ▼               ▼
  PostgreSQL      PostgreSQL      PostgreSQL      PostgreSQL
 (Schema condo)  (Schema xxx)    (Schema xxx)   (Schema xxx)
```

### 📡 Discovery Server (Eureka Server)
Utilizamos o **Spring Cloud Netflix Eureka** como nosso Service Registry na porta **8761**. Todos os microsserviços se registram dinamicamente ao iniciar, permitindo que a comunicação entre serviços ocorra pelo nome lógico (ex: `lb://iam-service`) em vez de IPs estáticos.

### 🎛️ API Gateway (Spring Cloud Gateway)
O API Gateway roda na porta **8080** como ponto único de entrada da aplicação, encaminhando as rotas externas para os nomes lógicos de serviço no Eureka:
* `/api/condominio/**` ➔ `lb://condominio-service` (com `StripPrefix=1`)
* `/api/iam/**` ➔ `lb://iam-service` (com `StripPrefix=1`)
* `/api/portaria/**` ➔ `lb://portaria-service` (com `StripPrefix=1`)
* `/api/notificacoes/**` ➔ `lb://notificacao-service` (com `StripPrefix=1`)
* `/api/auditoria/**` ➔ `lb://auditoria-service` (com `StripPrefix=1`)

---

## 💾 Persistência de Dados
Cada microsserviço gerencia seus próprios dados de forma isolada através de databases ou schemas PostgreSQL separados (`condominio`, `iam`, `portaria`, `notificacao`, `auditoria`).

---

## ⚡ Estratégia de Resiliência
Para evitar falhas em cascata na chamada síncrona do `condominio-service` para o `iam-service` (cadastro de credenciais), configuramos o **Resilience4j**:
* **Circuit Breaker:** Abre o circuito se mais de 50% das últimas 10 chamadas falharem ou estourarem o timeout, redirecionando o fluxo para um método de fallback amigável.
* **Time Limiter:** Aplica um timeout de 3 segundos nas chamadas ao IAM, impedindo que requisições presas esgotem os recursos do servidor Tomcat local.

---

## ⚡ Arquitetura TP2 — Observabilidade, Mensageria e Reatividade

Na segunda entrega (TP2), a arquitetura foi evoluída para incorporar três pilares essenciais de sistemas distribuídos modernos:

### 1. Observabilidade e Correlação (Correlation ID)
* **Correlation ID Tracing:** O **API Gateway** intercepta todas as requisições via `CorrelationGatewayFilter` (filtro reativo WebFlux). Se o header `X-Correlation-ID` não existir, ele gera um UUID, injeta-o na requisição enviada ao microsserviço correspondente e também na resposta HTTP final.
* **Logs Estruturados:** No `condominio-service`, o servlet `CorrelationFilter` captura o Correlation ID da requisição e o registra no **MDC (Mapped Diagnostic Context)** do SLF4J/Logback. O padrão de log do console (`[%X{correlationId}]`) é atualizado para exibir o ID de forma transparente em cada linha de log.
* **Métricas Customizadas:** Configuração do **Spring Boot Actuator** exposto em `/actuator/prometheus`. Usamos o **Micrometer** para coletar métricas customizadas de negócio (counters):
  * `condoplus.multas.aplicadas` — Incrementado ao aplicar multas.
  * `condoplus.comunicados.publicados` — Incrementado ao publicar comunicados.
  * `condoplus.reservas.confirmadas` — Incrementado ao confirmar reservas de áreas comuns.

### 2. Mensageria Assíncrona com Apache Kafka
* **Domínio Orientado a Eventos:** O `condominio-service` publica eventos de negócio de forma assíncrona para o broker:
  * `comunicados.publicados` contendo o payload `ComunicadoPublicadoEvent`.
  * `multas.aplicadas` contendo o payload `MultaAplicadaEvent`.
  * `reservas.confirmadas` contendo o payload `ReservaConfirmadaEvent`.
* **Envelope de Evento (`EventEnvelope`):** Todos os eventos publicados são encapsulados em um envelope padronizado que transporta metadados essenciais: `event_id`, `event_type`, `timestamp`, `correlation_id` (preservando o rastreamento do log), `origin_service` e o `payload` de negócio.
* **Resiliência e DLQ (Dead Letter Queue):** O tratamento de falhas em consumidores de Kafka é feito via `DeadLetterPublishingRecoverer` (Spring Kafka) configurado no `KafkaConfig`. Mensagens que falham repetidamente após 3 tentativas com 2 segundos de intervalo são enviadas automaticamente para o tópico `.DLT` (ex: `credenciais.criadas.DLT`).
* **Garantia de Idempotência:** O `CredencialCriadaConsumer` escuta o tópico `credenciais.criadas` e valida se a pessoa física já existe verificando previamente a existência do `documento` (CPF) ou do `credencial_id` no banco de dados, evitando duplicidades em caso de redelivery de mensagens.

### 3. Programação Reativa com WebFlux
* **WebClient Reativo:** O `IamClient` foi totalmente refatorado para utilizar o `WebClient` reativo do Spring WebFlux em substituição ao `RestClient` síncrono. O método `criarCredencial` retorna um `Mono<CredencialResponse>` manipulado pelo Project Reactor.
* **Tolerância a Falhas com Reactor:** O uso de `@CircuitBreaker` e `@TimeLimiter` do Resilience4j está integrado de forma nativa ao fluxo reativo do Project Reactor, acionando o método de `fallback` caso a chamada ao IAM exceda o tempo limite de 3 segundos ou o serviço esteja indisponível.

---

## 🛠️ Como Executar o Projeto

### Pré-requisitos:
* Java 21 JDK instalado.
* Maven 3.9+ instalado.
* Docker rodando localmente (para subir o banco de dados e os testes do Testcontainers).

### Ordem de Execução:

1. **Subir o Banco de Dados (PostgreSQL):**
   ```bash
   # Na raiz do projeto
   docker compose -f condominio_service/compose/docker-compose.yml up -d postgres
   ```

2. **Subir o Discovery Server (Eureka):**
   ```bash
   cd infra/eureka-server
   mvn spring-boot:run
   ```
   *Acesse `http://localhost:8761` no navegador para verificar a interface.*

3. **Subir o API Gateway:**
   ```bash
   cd ../api-gateway
   mvn spring-boot:run
   ```

4. **Subir o Condominio Service:**
   ```bash
   cd ../../condominio_service
   mvn spring-boot:run
   ```

5. **Subir os outros microsserviços (iam, portaria, notificacao, auditoria) a partir de suas respectivas branches.**

---

## 📡 Guia de Referência de APIs do Ecossistema

Esta seção serve como repositório de documentação de endpoints expostos no API Gateway (porta `8080`).

### 🏢 1. condominio-service (Prefix: `/api/condominio`)

#### 🏢 1.1 Unidades (`/api/condominio/unidades`)

#### Criar Unidade
* **Rota:** `POST http://localhost:8080/api/condominio/unidades`
* **Body:**
  ```json
  {
    "numero": "202",
    "bloco": "B",
    "tipo": "APARTAMENTO",
    "area_m2": 72.5
  }
  ```

#### Listar Todas as Unidades
* **Rota:** `GET http://localhost:8080/api/condominio/unidades`

#### Buscar Unidade por ID
* **Rota:** `GET http://localhost:8080/api/condominio/unidades/{unidadeId}`

---

### 👥 2. Pessoas (`/api/condominio/pessoas`)

#### Cadastrar Pessoa e Criar Credenciais no IAM
* **Rota:** `POST http://localhost:8080/api/condominio/pessoas`
* **Body:**
  ```json
  {
    "nomeCompleto": "Carlos Alberto",
    "email": "carlos@email.com",
    "senhaInicial": "senhaForte123",
    "documento": "98765432100",
    "telefone": "21988887777",
    "emailContato": "carlos.contato@email.com",
    "role": "MORADOR"
  }
  ```

#### Buscar Pessoa por ID
* **Rota:** `GET http://localhost:8080/api/condominio/pessoas/{pessoaId}`

#### Buscar Pessoa por CPF
* **Rota:** `GET http://localhost:8080/api/condominio/pessoas?cpf=98765432100`

---

### 🔗 3. Vinculações (`/api/condominio/unidades/{id}/vinculacoes`)

#### Criar Vinculação (Calcula escopos automaticamente)
* **Rota:** `POST http://localhost:8080/api/condominio/unidades/{unidadeId}/vinculacoes`
* **Body:**
  ```json
  {
    "pessoaId": "uuid-da-pessoa",
    "tipo": "RESIDENTE",
    "dataInicio": "2026-06-01"
  }
  ```

#### Listar Vinculações de uma Unidade
* **Rota:** `GET http://localhost:8080/api/condominio/unidades/{unidadeId}/vinculacoes?apenasAtivas=true`

#### Encerrar Vinculação
* **Rota:** `PATCH http://localhost:8080/api/condominio/vinculacoes/{vinculacaoId}/encerrar`
* **Body:**
  ```json
  {
    "dataFim": "2026-12-31"
  }
  ```

---

### 🚗 4. Veículos (`/api/condominio/unidades/{id}/veiculos`)

#### Cadastrar Veículo em uma Unidade
* **Rota:** `POST http://localhost:8080/api/condominio/unidades/{unidadeId}/veiculos`
* **Body:**
  ```json
  {
    "placa": "ABC1D23",
    "modelo": "Gol",
    "cor": "Branco",
    "proprietarioPessoaId": "uuid-da-pessoa"
  }
  ```

#### Listar Veículos de uma Unidade
* **Rota:** `GET http://localhost:8080/api/condominio/unidades/{unidadeId}/veiculos`

---

### 💼 5. Funcionários (`/api/condominio/funcionarios`)

#### Cadastrar Funcionário
* **Rota:** `POST http://localhost:8080/api/condominio/funcionarios`
* **Body:**
  ```json
  {
    "pessoaId": "uuid-da-pessoa",
    "cargo": "PORTEIRO",
    "dataAdmissao": "2026-01-15"
  }
  ```

#### Listar Funcionários
* **Rota:** `GET http://localhost:8080/api/condominio/funcionarios?cargo=PORTEIRO`

---

### 📣 6. Comunicados (`/api/condominio/comunicados`)

#### Publicar Comunicado (Apenas Síndico)
* **Rota:** `POST http://localhost:8080/api/condominio/comunicados`
* **Body:**
  ```json
  {
    "titulo": "Manutenção no Elevador",
    "conteudo": "O elevador do Bloco A ficará indisponível na segunda-feira pela manhã.",
    "visibilidade": "TODOS"
  }
  ```

#### Listar Comunicados
* **Rota:** `GET http://localhost:8080/api/condominio/comunicados?visibilidade=TODOS`

---

### ⚠️ 7. Multas (`/api/condominio/unidades/{id}/multas`)

#### Aplicar Multa a uma Unidade
* **Rota:** `POST http://localhost:8080/api/condominio/unidades/{unidadeId}/multas`
* **Body:**
  ```json
  {
    "descricao": "Uso indevido das áreas comuns",
    "valor": 150.00,
    "categoria": "CONVIVENCIA",
    "dataVencimento": "2026-07-01"
  }
  ```

#### Listar Multas de uma Unidade
* **Rota:** `GET http://localhost:8080/api/condominio/unidades/{unidadeId}/multas?status=PENDENTE`

#### Atualizar Status de uma Multa
* **Rota:** `PATCH http://localhost:8080/api/condominio/multas/{multaId}/status`
* **Body:**
  ```json
  {
    "status": "PAGA"
  }
  ```

---

### 🏊 8. Áreas Comuns e Reservas

#### Cadastrar Área Comum
* **Rota:** `POST http://localhost:8080/api/condominio/areas-comuns`
* **Body:**
  ```json
  {
    "nome": "Churrasqueira A",
    "descricao": "Espaço com churrasqueira e freezer",
    "capacidadeMaxima": 25,
    "regras": "Uso permitido das 09:00 às 22:00."
  }
  ```

#### Criar Reserva de Área Comum (Evita conflito de data/horário)
* **Rota:** `POST http://localhost:8080/api/condominio/reservas`
* **Body:**
  ```json
  {
    "areaComumId": "uuid-da-area-comum",
    "dataReserva": "2026-06-15",
    "horaInicio": "12:00",
    "horaFim": "18:00"
  }
  ```

---

### 🔑 2. iam-service (Espaço Reservado)
* **Desenvolvedor Responsável:** Integrante 2
* **Caminho Base no Gateway:** `/api/iam/**`

> [!NOTE]
> Espaço reservado para o desenvolvedor do `iam-service` documentar suas rotas e payloads (ex: registro de credenciais, login, validação de tokens).

* **Exemplo de Rota:** `POST http://localhost:8080/api/iam/...`
* **Exemplo de Payload:**
  ```json
  {}
  ```

---

### 🚪 3. portaria-service (Espaço Reservado)
* **Desenvolvedor Responsável:** Integrante 3
* **Caminho Base no Gateway:** `/api/portaria/**`

> [!NOTE]
> Espaço reservado para o desenvolvedor do `portaria-service` documentar suas rotas e payloads (ex: encomendas recebidas, liberação de visitantes/prestadores).

* **Exemplo de Rota:** `POST http://localhost:8080/api/portaria/...`
* **Exemplo de Payload:**
  ```json
  {}
  ```

---

### 🔔 4. notificacao-service (Espaço Reservado)
* **Desenvolvedor Responsável:** Integrante 4
* **Caminho Base no Gateway:** `/api/notificacoes/**`

> [!NOTE]
> Espaço reservado para o desenvolvedor do `notificacao-service` documentar suas rotas e payloads (ex: disparo de alertas, emails, sms e logs).

* **Exemplo de Rota:** `POST http://localhost:8080/api/notificacoes/...`
* **Exemplo de Payload:**
  ```json
  {}
  ```

---

### 📊 5. auditoria-service (Espaço Reservado)
* **Desenvolvedor Responsável:** Integrante 5
* **Caminho Base no Gateway:** `/api/auditoria/**`

> [!NOTE]
> Espaço reservado para o desenvolvedor do `auditoria-service` documentar suas rotas e payloads (ex: gravação e consulta de logs estruturados de auditoria).

* **Exemplo de Rota:** `POST http://localhost:8080/api/auditoria/...`
* **Exemplo de Payload:**
  ```json
  {}
  ```