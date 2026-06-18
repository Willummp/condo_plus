# auditoria-service

Serviço de auditoria do Condo+. Registra, de forma centralizada e imutável,
os eventos relevantes que acontecem nos demais microsserviços (multas
aplicadas, acessos na portaria, logins, encomendas, etc.), permitindo
consulta e rastreabilidade posterior.

É o serviço não-relacional do projeto: persiste em MongoDB.

## Stack

- Java 21 (Eclipse Temurin)
- Spring Boot 3.3.5 (Web, Data MongoDB, Security, Validation, Actuator)
- MongoDB 7.0 (via Docker)
- Maven (módulo do monorepo Condo+)
- Porta: 8085

## Como subir

1. Subir o MongoDB (a partir da raiz do monorepo):

   docker compose -f auditoria_service/compose/docker-compose.yml up -d

   Confirme com: docker compose -f auditoria_service/compose/docker-compose.yml ps
   (status deve ser "healthy")

2. Rodar a aplicação:

   Pelo IntelliJ (botão Run em AuditoriaServiceApplication) ou via Maven.
   Aguarde a linha "Started AuditoriaServiceApplication" no log.

## Endpoints

| Método | Caminho                                                        | Descrição                                  |
|--------|----------------------------------------------------------------|--------------------------------------------|
| POST   | /auditoria/registros                                           | Registra um evento auditável               |
| GET    | /auditoria/registros?tipoEvento=&page=&size=                   | Lista registros (filtro e paginação)       |
| GET    | /auditoria/registros/historico?tipoEntidade=&idEntidade=       | Histórico de uma entidade específica       |
| GET    | /actuator/health                                               | Health-check                               |

### Exemplo de evento (POST)

```json
{
  "eventId": "evt-teste-001",
  "correlationId": "corr-abc-123",
  "timestamp": "2026-06-16T05:00:00Z",
  "tipoEvento": "MULTA_APLICADA",
  "servicoOrigem": "condominio-service",
  "entidadeAfetada": { "tipo": "Multa", "id": "multa-42" },
  "payload": { "valor": 150.0, "motivo": "barulho apos 22h" }
}
```

## Como testar

Com a aplicação rodando e um evento salvo em evento-teste.json:

```bash
# 1. Grava um evento (espera 201 Created)
curl.exe -H "Content-Type: application/json" -d "@evento-teste.json" http://localhost:8085/auditoria/registros

# 2. Grava o MESMO evento de novo: a resposta traz o mesmo id (idempotência)
curl.exe -H "Content-Type: application/json" -d "@evento-teste.json" http://localhost:8085/auditoria/registros

# 3. Lista os registros (totalElements continua 1, provando a deduplicação)
curl.exe http://localhost:8085/auditoria/registros

# 4. Histórico de uma entidade específica
curl.exe "http://localhost:8085/auditoria/registros/historico?tipoEntidade=Multa&idEntidade=multa-42"
```

## Decisões de arquitetura

- **MongoDB (não relacional):** cada tipo de evento tem um payload diferente
  (uma multa tem valor/motivo; um login tem ip/userAgent). O modelo documental
  absorve essa heterogeneidade num campo `payload` flexível, sem explodir em
  tabelas nem perder garantias. O serviço também é write-heavy (insere muito,
  atualiza quase nunca) e usa TTL nativo — perfil que casa com MongoDB.

- **Idempotência via índice único em `eventId`:** o registro grava direto e
  confia na constraint do banco. Se o mesmo `eventId` chegar de novo (ex.:
  redelivery do Kafka no TP2), o Mongo recusa com DuplicateKeyException, que é
  tratada como sucesso silencioso, devolvendo o registro existente. Evita o
  padrão "checar-depois-salvar", que tem condição de corrida sob concorrência.

- **TTL nativo (365 dias):** o índice com expireAfterSeconds faz o MongoDB
  apagar registros antigos automaticamente, sem job/cron externo.

- **Índices compostos:** as consultas (histórico por entidade; eventos por
  serviço numa janela) usam índices compostos dedicados, evitando varredura.

- **DTOs na borda:** o cliente não envia nem recebe a entidade de persistência
  direto. Campos internos (id, dataInsercao) são controlados pelo servidor.

- **Segurança simples no TP1:** este serviço libera seus endpoints internamente;
  a autenticação forte (validação de JWT) vive no API Gateway, que protege a
  borda antes da requisição chegar aqui. Configuração stateless, sem sessão.

## Limitações conscientes (TP1)

- A duplicata retorna 201 (e não 200). A prova da idempotência é o id repetido
  na resposta, o WARN no log e o totalElements estável.
- `auto-index-creation` está ligado: cómodo em dev, mas em produção criar índice
  em coleção grande pode bloquear escritas — migraria para criação controlada.
- Sem testes automatizados ainda (planejados com Testcontainers).

---

# TP2 — Mensageria, deteccao de anomalias e observabilidade

No TP1 o servico recebia eventos por REST (POST sincrono). No TP2 ele passa a
consumir eventos de forma assincrona via Apache Kafka: os demais servicos
publicam o que aconteceu, e o auditoria arquiva e analisa sem acoplar ninguem a
uma chamada HTTP. O endpoint REST do TP1 continua existindo (util para testes e
para registros pontuais), mas o fluxo principal agora e orientado a eventos.

## Arquitetura de mensageria

O servico assina oito topicos, um por tipo de evento de negocio
(`multas.aplicadas`, `acessos.registrados`, `logins.falhados`, etc.). Cada
topico tem seu proprio metodo `@KafkaListener`, todos no mesmo `groupId`
(`auditoria-group`).

- **Um listener por topico, em vez de um listener generico:** deixa explicito
  no codigo quais eventos o auditoria entende, e permite tratar cada tipo de
  forma diferente no futuro sem reescrever um roteador central.
- **`groupId` unico:** garante que, se um dia houver mais de uma instancia do
  auditoria, o Kafka distribui as particoes entre elas (cada evento e
  processado por uma so instancia), sem duplicar arquivamento.
- **Offset reset `earliest`:** se o auditoria sobe depois dos eventos ja terem
  sido publicados, ele le desde o inicio do topico em vez de perder o historico.
  Para um servico de auditoria, perder evento e o pior cenario possivel.
- **`ErrorHandlingDeserializer` + DLT:** se chega uma mensagem malformada, ela
  nao derruba o consumer nem trava a particao; vai para uma Dead Letter Topic e
  o processamento segue. Segue o padrao ja adotado pelo condominio-service.

## Contrato de leitura proprio

O auditoria define seu proprio `EventEnvelope` (record imutavel), em vez de
importar a classe de evento do condominio-service.

- **Por que nao reaproveitar a classe do produtor:** isso acoplaria o auditoria
  ao modelo de dominio de outro servico. Toda mudanca la poderia quebrar aqui, e
  o auditoria consome eventos de *todos* os servicos — nao da para depender da
  hierarquia de classes de cada um.
- **`setUseTypeHeaders=false`:** por padrao o Spring Kafka tenta desserializar
  para a classe indicada num header da mensagem (que aponta para a classe do
  produtor). Desligando isso, o auditoria desserializa sempre para o *seu*
  envelope, ignorando de quem a mensagem veio. E o ponto-chave do desacoplamento.
- **`@JsonIgnoreProperties(ignoreUnknown = true)`:** se um produtor adiciona um
  campo novo no evento, o auditoria simplesmente ignora em vez de quebrar. O
  `payload` e um `Map<String, Object>` generico: qualquer formato cabe.

## Idempotencia no consumo

O Kafka garante entrega *at-least-once*: a mesma mensagem pode chegar mais de uma
vez (redelivery apos um rebalance, por exemplo). O arquivamento precisa ser
idempotente.

- **Mesmo mecanismo do TP1, agora exercitado de verdade:** o indice unique em
  `eventId` + o tratamento de `DuplicateKeyException` como sucesso silencioso.
  No TP1 isso era prevencao; no TP2, com redelivery real do Kafka, vira
  requisito de corretude. A decisao de design do TP1 se pagou aqui.

## Deteccao de anomalias plugavel

Alem de arquivar, o servico analisa os eventos em busca de padroes suspeitos
(ex.: muitas falhas de login em pouco tempo). A deteccao foi desenhada para ser
extensivel.

- **Interface `RegraAnomalia` + orquestrador `DetectorAnomalias`:** cada regra e
  uma classe que implementa a interface; o detector recebe todas as regras
  injetadas pelo Spring e aplica cada uma ao evento. Adicionar uma regra nova e
  criar uma classe — nao se toca no detector nem nas regras existentes
  (principio Open/Closed). No startup o log confirma quantas regras foram
  carregadas.
- **Janela deslizante relativa ao timestamp do evento, nao ao relogio do
  servidor:** uma regra como "5 logins falhos em 60s" mede o intervalo entre os
  eventos pelo `timestamp` que veio neles, nao pela hora atual da maquina. Assim
  a deteccao funciona igual se os eventos forem reprocessados depois (replay),
  e nao depende de o servidor estar com o relogio certo.
- **Comparacao por igualdade (`==`) no limiar, nao `>=`:** a anomalia e criada
  exatamente quando a contagem atinge o limite, uma unica vez. Com `>=` cada
  evento acima do limite geraria um novo alerta, inundando o sindico de
  notificacoes para o mesmo incidente.
- **Severidade graduada:** a mesma regra emite WARNING ou CRITICAL conforme a
  intensidade (ex.: 5 falhas e atencao; 10 e critico), dando ao sindico uma
  nocao de prioridade.

## Triagem de anomalias

As anomalias detectadas nao sao so registradas: o sindico precisa poder
visualiza-las e marca-las como tratadas.

- **GET com filtro por status e paginacao; PATCH para mudar o status:** uma
  anomalia nasce ABERTA e pode ser movida para RECONHECIDA (o sindico viu) ou
  outro estado. O PATCH atualiza so o status, validado na entrada; id inexistente
  responde 404.
- **Por que separar deteccao de triagem:** detectar e automatico e continuo;
  triar e uma acao humana. Modelar isso como um campo de status editavel mantem
  o registro da anomalia intacto (auditavel) enquanto reflete a decisao do
  operador.

## Observabilidade

- **`correlation_id` ponta a ponta:** um filtro le o header `X-Correlation-ID`
  nas requisicoes REST (gera um se faltar) e o coloca no MDC do log; no consumo
  Kafka, o mesmo id vem dentro do envelope e e propagado da mesma forma. Assim um
  unico evento pode ser rastreado por toda a cadeia de logs, mesmo cruzando de
  REST para mensageria. O MDC e limpo no fim de cada processamento porque as
  threads sao reutilizadas (Tomcat e os listeners), e um id vazado contaminaria
  a proxima requisicao.
- **Metricas no formato Prometheus:** o detector incrementa um contador
  (`auditoria_anomalias_detectadas_total`) com tags de tipo de regra e
  severidade, exposto em `/actuator/prometheus`. Permite acompanhar quantas
  anomalias de cada tipo o servico detecta ao longo do tempo, sem ler log.
- **Actuator com exposicao cirurgica:** so os endpoints necessarios
  (`health`, `info`, `metrics`, `prometheus`) sao liberados no SecurityConfig —
  nao `/actuator/**` inteiro, que exporia endpoints sensiveis.

## Testes automatizados

O TP2 fecha a lacuna deixada no TP1 (que nao tinha testes). Ha um teste de
integracao do fluxo de arquivamento: consumo -> mapeamento -> persistencia
idempotente -> MongoDB.

- **MongoDB embarcado (flapdoodle), nao Testcontainers:** o plano original do
  TP1 citava Testcontainers, mas ele exige um ambiente Docker que o
  Testcontainers consiga detectar — o que nao funcionou no ambiente de
  desenvolvimento (Windows + Docker Desktop). O flapdoodle sobe um MongoDB real
  em memoria sem depender de Docker, rodando igual na maquina local e num runner
  de CI. A versao do Mongo embarcado e fixada para casar com a major usada em
  producao (7.0), entao o teste exercita os mesmos indices e o mesmo TTL.
- **Chamada direta ao consumer, sem broker Kafka embarcado:** subir um broker em
  memoria (`@EmbeddedKafka`) se mostrou instavel no Windows. Em vez disso, o
  teste invoca o `EventoConsumer` diretamente com um envelope montado. Isso
  exercita exatamente a logica de negocio (mapeamento + idempotencia +
  persistencia) de forma deterministica, sem timing nem dependencia de
  transporte. A serializacao Kafka em si foi validada ao vivo durante o
  desenvolvimento, via console-producer.
- **Dois casos:** um evento consumido vira um registro no Mongo; o mesmo evento
  consumido duas vezes gera um unico registro (idempotencia garantida pelo indice
  unique de `eventId`).

## Limitacoes conscientes (TP2)

- **Paginacao serializando `PageImpl` diretamente:** os endpoints de listagem
  retornam `Page<...>` serializado como esta, o que gera um aviso do Spring de
  que o formato JSON nao e estavel entre versoes. Para o escopo do trabalho e
  aceitavel; em producao migraria para um DTO de pagina explicito
  (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`).
- **Eureka desligado no perfil de desenvolvimento:** o `eureka.client.enabled`
  esta `false` enquanto o servidor de descoberta do grupo nao esta de pe.
  Reativar e so inverter a flag quando o Eureka estiver disponivel.
- **Teste cobre a logica de arquivamento, nao o transporte:** o teste valida o
  comportamento do servico (arquivar, deduplicar), nao a infraestrutura Kafka
  (rebalance, offsets) — que e responsabilidade do framework e foi verificada
  manualmente.