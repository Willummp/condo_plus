# 📚 Documentação de Estudos Profunda — TP2 (Eventos, Observabilidade e Reatividade)

Este guia foi desenhado para consolidar a teoria profunda e a prática das modernizações exigidas no TP2 do Condo+. Entenda o "Por que" e o "O que", antes de ver o "Onde".

---

## 1. Observabilidade: Correlation ID e Actuator

### O que é Observabilidade?
Imagine um erro estourando no painel do administrador. Como descobrir a origem se a requisição passou pelo Gateway, pelo IAM, chamou o Kafka, e foi parar no Serviço de Condomínios? **Observabilidade** é tornar os "órgãos internos" do sistema visíveis.

### O que é Correlation ID (ID de Correlação) e MDC?
Quando a requisição do usuário entra no Gateway, o sistema cria uma etiqueta única (ex: `123-abc`). Essa etiqueta é o **Correlation ID**. Ela viaja pelos "Cabeçalhos" HTTP por todos os microsserviços. 
- **O problema do Log:** Se você apenas receber a etiqueta, você teria que digitar `logger.info("Etiqueta: " + id + " processando")` em todas as milhares de linhas do projeto. Isso é insano.
- **A Solução (MDC):** O **Mapped Diagnostic Context (MDC)** é um recurso da biblioteca de Logs (Logback). Assim que a requisição chega no Microsserviço, um Filtro pega a Etiqueta e "grampeia" ela na *Thread* atual do Java (`MDC.put()`). A partir daquele milissegundo, absolutamente qualquer `log.info()` disparado no sistema irá magicamente imprimir a Etiqueta automaticamente. Quando a requisição morre, o MDC joga a etiqueta fora.

### O que é Spring Boot Actuator e Micrometer?
- **Actuator:** É uma ferramenta do Spring que injeta endpoints prontos no seu projeto (como `/actuator/health` ou `/actuator/metrics`). Ele serve como o painel do carro do seu app, exibindo se está ligado, memória RAM usada, e etc.
- **Micrometer:** Assim como o `SLF4J` é a interface padrão para escrever textos (Logs), o Micrometer é a interface padrão para escrever **Métricas** numéricas (ex: "Contador de Multas Aplicadas"). O Micrometer coleta esses números e formata perfeitamente para sistemas de monitoramento modernos, como o **Prometheus**.

📍 **Onde encontrar no código:**
- **MDC:** Em [CorrelationFilter.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/config/CorrelationFilter.java)
- **Métricas:** Em [ComunicadoService.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/convivencia/service/ComunicadoService.java) (veja `Counter.builder(...)`)

---

## 2. Comunicação Assíncrona com Apache Kafka

### Por que trocar o REST síncrono pelo Kafka assíncrono?
Se o Serviço de Moradores (REST) precisar avisar o Serviço Financeiro que um morador novo entrou, ele manda um HTTP POST. Se o Financeiro estiver travado, o Serviço de Moradores trava esperando.
**Com o Kafka (Broker de Mensagens):** O Serviço de Moradores simplesmente grita num megafone: *"UM MORADOR ENTROU!"* (Ele **Produziu** um Evento de Domínio). Ele não liga para quem está ouvindo e continua seu trabalho. O Serviço Financeiro, quando estiver livre, escuta a mensagem (Ele **Consome**) e faz o que precisa. O sistema nunca trava por causa do vizinho.

### Produtor, Consumidor e Idempotência
- **Produtor:** Quem envia a mensagem para o tópico do Kafka.
- **Consumidor:** Quem fica de olho no tópico para baixar e processar mensagens novas.
- **A Idempotência:** A regra de ouro de sistemas distribuídos é: "A rede sempre falha". Às vezes o consumidor processa a mensagem, mas a rede cai antes dele avisar o Kafka. O Kafka manda a mensagem de novo. O seu Consumidor deve ser **Idempotente**: ele deve verificar se o "CPF já existe" antes de salvar. Fazer a mesma ação 1 ou 1000 vezes tem que resultar no mesmo estado final (sem gerar 1000 moradores duplicados).

### A Fila Morta (DLT - Dead Letter Topic)
Se a mensagem recebida for um JSON corrompido, seu código dará Erro. O Kafka, teimoso, manda de novo. Seu código dá Erro. O servidor entra num laço infinito processando um veneno.
**A Solução (DLT):** Configuramos o Spring para tentar apenas 3 vezes. Se falhar na 3ª, o Spring remove a mensagem da fila principal e joga numa "Fila Morta". O sistema continua processando o resto livremente, e o administrador olha a fila morta depois.

📍 **Onde encontrar no código:**
- **Produtor:** [CondominioEventProducer.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/producer/CondominioEventProducer.java)
- **Consumidor (Idempotente):** [CredencialCriadaConsumer.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/consumer/CredencialCriadaConsumer.java)
- **DLT:** Em [KafkaConfig.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/config/KafkaConfig.java)

---

## 3. Programação Reativa: WebClient vs RestTemplate

### O problema do paradigma Síncrono (Thread-per-request)
O modelo tradicional do Java aloca uma "Thread" (operário de CPU) inteira para uma requisição de usuário. Se você faz uma chamada HTTP para outro sistema que demora 3 segundos, o seu operário (Thread) fica 3 segundos cruzando os braços, parado, dormindo. Se chegarem 10.000 requisições lentas, suas 10.000 threads travam. O servidor infarta por falta de Threads livres, mesmo a CPU estando a 10%.

### A solução Reativa (Project Reactor)
Com a **Programação Reativa**, a Thread envia a requisição HTTP e diz: *"Me avise quando a resposta chegar"*, e vai imediatamente atender outro usuário (Event Loop não-bloqueante). Uma única Thread atende milhares de clientes.
- **WebClient:** É o cliente HTTP reativo do Spring WebFlux (substituto do velho `RestTemplate`).
- **Mono:** No reativo, você não retorna o objeto final (ex: `String`). Você retorna uma promessa de que aquilo vai existir no futuro (o `Mono<String>` - "Um elemento no futuro" ou `Flux<String>` - "Vários elementos no futuro").

📍 **Onde encontrar no código:**
- **Cliente Reativo:** Em [IamClient.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/main/java/com/condoplus/condominio/estrutura/client/IamClient.java) (Veja o tipo de retorno `Mono` e a declaração de `webClient`).

---

## 4. Testcontainers no Kafka

Semelhante ao TP1, onde subimos um banco PostgreSQL real durante os testes automatizados, fazemos a exata mesma coisa com o **Apache Kafka**.
Ao invocar o comando `@Testcontainers` no JUnit, o Docker levanta um contêiner real (`confluentinc/cp-kafka`), o Spring conecta as configurações locais nele, testa as rotinas de publicação do Produtor, e logo após, derruba a infraestrutura limpamente. Garantia total contra falsos-positivos.

📍 **Onde encontrar no código:**
- **Teste de Integração:** Em [CondominioKafkaIT.java](file:///c:/Users/lucferreir/OneDrive - Globo Comunicação e Participações sa/Documentos/Dev/condo_plus/condominio_service/src/test/java/com/condoplus/condominio/integration/CondominioKafkaIT.java) (`@Container static KafkaContainer`).
