# Trabalho Prático 2 - Notificação Service (Mensageria e Observabilidade)

Este documento detalha a implementação dos requisitos de Observabilidade (Métricas, Logs estruturados e Correlação) e a 
arquitetura de Mensageria assíncrona com Apache Kafka no microsserviço `notificacao-service`.

## 3. Observabilidade

### 3.1 Métricas (Spring Boot Actuator e Prometheus)
O microsserviço expõe suas métricas nativas e de negócio no endpoint `/actuator/prometheus`. 
Através da dependência `micrometer-registry-prometheus`, os dados de telemetria são raspados pelo servidor do Prometheus 
com as seguintes capacidades:

**Quantidade de requisições recebidas e Tempo médio de resposta:** Coletados via métricas HTTP padrão do Spring WebFlux. O histograma de distribuição está configurado no `application.yml` para calcular percentis de latência (0.5, 0.95 e 0.99):
  `http_server_requests_seconds_count` e `http_server_requests_seconds_sum`
**Quantidade de erros:** Monitorada através do status HTTP ou falhas reativas capturadas nas métricas sob a tag `exception` ou `status=5xx`.
**Chamadas para outros microservices:** Métricas de saída geradas nas requisições do `CondominioWebClient` ao interagir com o `condominio-service`.
* **Eventos consumidos do Kafka:** Telemetria nativa fornecida pelas propriedades do cliente do `reactor-kafka` que monitoram lag e mensagens lidas por segundo.
* **Estado de circuit breakers:** Métricas expostas pelo Resilience4j configurado no `application.yml` sob a instância `condominioService`, indicando se o circuito está em estado `CLOSED`, `OPEN` ou `HALF_OPEN`.
* **Métricas de negócio (Notificações Enviadas):** O sistema monitora o sucesso e falha das notificações alterando os estados no PostgreSQL reativo para `StatusNotificacao.ENVIADA` ou `StatusNotificacao.FALHOU` através das classes `DespacharService` e `NotificacaoService`.

---

### 3.2 Logs Estruturados
O sistema utiliza o Logback configurado via `logback-spring.xml` para imprimir mensagens claras no console em formato 
plano para o ambiente de desenvolvimento (`dev`), permitindo auditoria instantânea das seguintes perguntas:

* **Qual requisição chegou? / Qual operação foi executada?**
    * *Exemplo:* `[INFO] Recebendo solicitacao REST para pessoaId=11111111-1111-1111-1111-111111111111`
* **Houve erro?**
    * *Exemplo:* `[ERROR] Erro processando ComunicadoPublicado. NAO confirmando para o Kafka redeliverar.`
* **Qual serviço chamou qual serviço?**
    * *Exemplo:* `[INFO] Chamando CONDOMINIO-SERVICE via WebClient para unidadeId=22222222-2222-2222-2222-222222222222`
* **Qual evento Kafka foi publicado ou consumido?**
    * *Exemplo:* `[INFO] ComunicadoPublicado recebido via Kafka: payload={"comunicadoId": 45, "titulo": "Reuniao"}`
* **Qual identificador permite correlacionar uma operação entre serviços?**
    * O campo `[%X{correlationId:-}]` injetado no padrão do encoder do `logback-spring.xml`.

---

### 3.3 Correlação e Rastreamento (Trace e Correlation ID)
A estratégia adota a propagação de um **Correlation ID** de forma transversal:
1. Ao receber uma mensagem no barramento do Kafka ou requisição HTTP, o identificador único é capturado ou gerado (ex: `evt-` + timestamp).
2. O identificador é inserido no **MDC (Mapped Diagnostic Context)** do SLF4J, fazendo com que todos os logs gerados na mesma Thread ou cadeia reativa imprimam o ID automaticamente entre colchetes.
3. *Demonstração de Log com ID de Correlação:*
   `2026-06-21 00:05:12,123 [parallel-1] INFO  [corr-id-abc-123] com.condoplus.notificacao.messaging.ComunicadoConsumer - ComunicadoPublicado recebido via Kafka: payload=...`

---

## 4. Apache Kafka e Comunicação Assíncrona

### 4.1 Eventos de Domínio Mapeados
O ecossistema do Condo+ utiliza eventos baseados em fatos de negócio ocorridos no passado. O `notificacao-service` atua estritamente como **Consumidor** dos seguintes tópicos globais:
* **`comunicados.publicados`** (Gatilho para disparar avisos gerais ou por bloco aos moradores).
* **`multas.aplicadas`** (Gatilho para notificar um condômino sobre infrações).
* **`encomendas.recebidas`** (Gatilho para avisar que uma encomenda chegou na portaria).
* **`reservas.confirmadas`** (Gatilho de aviso sobre a aprovação de áreas comuns).

---

### 4.2 Produtor e Consumidor (Estrutura do Projeto)
* **Consumidor Reativo Implementado:** Pacote `com.condoplus.notificacao.messaging` contendo as classes `ComunicadoConsumer`, `MultaConsumer`, `EncomendaConsumer` e `ReservaConsumer`.
* **Tópicos Escutados:** Configurados via anotação `@KafkaListener` nas rotas: `comunicados.publicados`, `multas.aplicadas`, `encomendas.recebidas` e `reservas.confirmadas`.
* **Fluxo Funcional Demonstrável:** O barramento entrega o payload JSON para o Consumer -> O Consumer converte os dados para o DTO `EventoNotificacao` -> Invoca o `NotificacaoService` para realizar o Fan-out (cruzamento com as preferências do banco PostgreSQL) -> Dispara o `DespacharService` para simular o envio assíncrono não-bloqueante no canal adequado (E-mail, WhatsApp, Push ou In-App).

---

### 4.3 Justificativa do uso do Kafka
Chamadas HTTP diretas são síncronas e bloqueantes. Se o serviço de portaria precisasse notificar o morador via HTTP direto e a API externa de e-mail estivesse lenta ou fora do ar, a portaria inteira travaria aguardando a resposta, impedindo a entrada de carros ou entrega de outras encomendas.

Com o Kafka, a portaria apenas publica o evento e volta a operar instantaneamente. O `notificacao-service` consome o evento em segundo plano de forma totalmente assíncrona, garantindo o **desacoplamento temporal** e a **resiliência** global do condomínio.

---

### 4.4 Cuidados Esperados e Resiliência
O projeto mitiga falhas arquiteturais através das seguintes táticas seniores:

* **O que acontece se o consumidor estiver fora do ar:** Configurado com commit manual (`ack-mode: MANUAL_IMMEDIATE`) no `application.yml`. O Kafka retém as mensagens nos seus discos. Quando o `notificacao-service` for reiniciado, ele processará as mensagens pendentes de forma retroativa a partir do último offset confirmado.
* **Como evitar inconsistências e registrar erros no consumo:** O commit manual (`ack.acknowledge()`) só é acionado dentro do operador `.doOnComplete()` do fluxo reativo. Se ocorrer um erro ou falha na persistência do banco de dados, o sinal `.doOnError()` captura a exceção, o log é registrado no console e o commit **NÃO** é enviado ao Kafka. Assim, o barramento detecta que a mensagem não foi processada e tenta a redelivery (reentrega) de forma segura.
* **Garantia Idempotente:** O método `criarOuRecuperarNotificacao` trata a exceção de chave duplicada (`DataIntegrityViolationException`), garantindo que se uma mensagem for processada mais de uma vez devido a uma reentrega do Kafka, o sistema recupera o registro existente em vez de duplicar a notificação para o morador.
