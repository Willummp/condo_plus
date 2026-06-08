# 📚 Documentação de Estudos — TP2 (Eventos, Observabilidade e Reatividade)

Este documento descreve os conceitos teóricos e práticos abordados no **Trabalho Prático 2 (TP2)** para evolução do ecossistema do **Condo+**, com referências claras de onde encontrar cada implementação no código.

---

## 1. Observabilidade e Rastreamento Distribuído (Correlation ID)

Para evitar que um erro numa rede de microsserviços fique impossível de rastrear, usamos um identificador unificado.

### A. Correlação (Correlation ID) com MDC
* O API Gateway gera o ID se não existir e propaga no header `X-Correlation-ID`.
* O `condominio-service` captura o header e anexa à thread usando o **MDC (Mapped Diagnostic Context)** do Logback, o que faz todas as linhas de log geradas pela thread imprimirem o ID.

📍 **Onde encontrar no código:**
- **Gateway:** [CorrelationGatewayFilter.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/infra/api-gateway/src/main/java/com/condoplus/gateway/filter/CorrelationGatewayFilter.java) (Interceptação e injeção do header).
- **Serviço:** [CorrelationFilter.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/config/CorrelationFilter.java) (Leitura do header e registro no `MDC.put()`).

### B. Métricas com Actuator e Micrometer
* Uso do **Spring Boot Actuator** e exportação via `/actuator/prometheus`.
* Métricas customizadas de negócio (ex: número de reservas e multas).

📍 **Onde encontrar no código:**
- **Contadores (Counters):** Em [ComunicadoService.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/convivencia/service/ComunicadoService.java), veja `Counter.builder("condoplus.comunicados.publicados").register(meterRegistry)` e a chamada `comunicadosCounter.increment()`. O mesmo ocorre em [MultaService.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/convivencia/service/MultaService.java).

---

## 2. Comunicação Assíncrona com Apache Kafka

Evita acoplamento temporal e falhas de comunicação bloqueantes entre os microsserviços.

### A. Domínio Orientado a Eventos (Domain Events) e Produtores
* Eventos de domínio são fatos que ocorreram no passado do sistema (ex: *ReservaConfirmada*, *MultaAplicada*). 
* Um `Producer` (Produtor) é acionado pelos serviços principais assim que uma entidade é gravada no banco.

📍 **Onde encontrar no código:**
- **Classes de Eventos (Records):** Pasta [event/](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/event), como [ReservaConfirmadaEvent.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/event/ReservaConfirmadaEvent.java).
- **Produtor Kafka:** [CondominioEventProducer.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/producer/CondominioEventProducer.java) (onde invocamos `kafkaTemplate.send()`).

### B. Consumidor e Idempotência
* O Consumidor reage de forma passiva a mensagens (ex: *CredencialCriadaEvent* vinda do IAM).
* **Idempotência:** Ele checa se o documento CPF da credencial já foi cadastrado antes de salvar a pessoa de novo, evitando que um erro de rede duplique registros.

📍 **Onde encontrar no código:**
- **Consumidor:** Em [CredencialCriadaConsumer.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/consumer/CredencialCriadaConsumer.java) (veja o `@KafkaListener` e o `if (pessoaRepository.existsByDocumento(event.documento()))`).

### C. Resiliência no Kafka: DLQ (Dead Letter Queue)
* Se a mensagem causar uma exceção ao ser consumida, ela é retentada e depois jogada para um tópico DLT (Dead Letter Topic) para análise humana, impedindo o travamento da fila.

📍 **Onde encontrar no código:**
- **Configuração da DLT:** Em [KafkaConfig.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/config/KafkaConfig.java) (utilizando o `DeadLetterPublishingRecoverer` com um BackOff de 3 tentativas).

---

## 3. Programação Reativa com WebFlux e WebClient

Troca do `RestClient` síncrono pelo paradigma reativo, que não bloqueia threads em chamadas I/O (Input/Output).

📍 **Onde encontrar no código:**
- **Cliente HTTP Reativo:** Em [IamClient.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/estrutura/client/IamClient.java), veja o uso da biblioteca `org.springframework.web.reactive.function.client.WebClient` do Spring WebFlux e o retorno empacotado no objeto reativo `Mono<CredencialResponse>`.
- **Inscrição Síncrona Segura:** Em [PessoaService.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/estrutura/service/PessoaService.java), veja o `.block()` sendo usado para compatibilizar o fluxo reativo com o atual processo síncrono da transação JDBC.

---

## 4. Testes do Kafka com Testcontainers

Semelhante ao que foi feito com PostgreSQL, subir um container real do Kafka para testar a mensageria de forma isolada durante as pipelines de Integração.

📍 **Onde encontrar no código:**
- **Teste de Integração:** Em [CondominioKafkaIT.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/test/java/com/condoplus/condominio/integration/CondominioKafkaIT.java), declarando a inicialização do `@Container static KafkaContainer kafka`.
