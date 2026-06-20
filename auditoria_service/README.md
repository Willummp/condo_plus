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

Com a aplicação rodando, a partir da raiz do monorepo:

```bash
# 1. Grava um evento (espera 201 Created)
curl.exe -H "Content-Type: application/json" -d "@auditoria_service/test-manual/evento-teste.json" http://localhost:8085/auditoria/registros

# 2. Grava o MESMO evento de novo: a resposta traz o mesmo id (idempotência)
curl.exe -H "Content-Type: application/json" -d "@auditoria_service/test-manual/evento-teste.json" http://localhost:8085/auditoria/registros

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


---

# TP3 — Protecao de rotas

No TP3 o servico passa a proteger suas rotas: requisicoes sem identidade valida
sao rejeitadas, e a triagem de anomalias passa a exigir um papel especifico.

## Onde mora a autenticacao (e por que nao aqui)

A autenticacao propriamente dita — login, emissao de JWT e renovacao de token
(refresh) — e responsabilidade do componente de autenticacao do ecossistema (o
auth-service / API Gateway), nao do auditoria. Este servico e um **consumidor**
dessa autenticacao: ele nao emite nem valida token; recebe a identidade do
usuario ja validada e protege suas rotas com base nela.

- **Por que essa separacao importa:** numa arquitetura de microsservicos, validar
  o JWT em todo servico duplicaria a mesma logica em varios lugares e espalharia
  a chave de assinatura por todo o sistema. Concentrar a validacao na borda (o
  Gateway) significa um unico ponto que conhece como validar token; os servicos
  internos confiam na identidade que ele propaga. O auditoria fazer login ou
  emitir token, alem disso, violaria sua responsabilidade unica — ele audita, nao
  autentica.

## Como a protecao funciona aqui

O Gateway valida o JWT e, ao encaminhar a requisicao para o servico, injeta a
identidade do usuario em dois headers: `X-User-Id` e `X-User-Roles` (papeis
separados por virgula, ex.: `SINDICO,MORADOR`).

- **`GatewayAuthenticationFilter`:** um filtro le esses headers a cada requisicao.
  Se o `X-User-Id` esta presente, popula o contexto de seguranca do Spring com o
  usuario e seus papeis (cada papel vira uma authority `ROLE_<papel>`). Se esta
  ausente, o contexto fica vazio.
- **Confianca com verificacao:** o servico confia no Gateway, mas exige a prova de
  que a requisicao passou por ele — a presenca do `X-User-Id`. Uma requisicao que
  chega sem esse header (ou seja, sem ter passado pelo Gateway) nao autentica e e
  rejeitada. O servico nao revalida o token porque isso ja foi feito na borda;
  ele apenas confere que a identidade foi propagada.
- **Stateless:** nenhuma sessao e criada; cada requisicao carrega sua propria
  identidade nos headers. Coerente com um microsservico atras de Gateway.

## Autorizacao por papel

A protecao nao e uniforme — varia conforme o ator de cada rota:

- **Rotas de auditoria (gravar e consultar registros):** sao trafego entre
  servicos (os demais microsservicos publicam eventos; o sistema consulta o
  historico). Exigem apenas estar autenticado, isto e, ter passado pelo Gateway.
- **Triagem de anomalia (`PATCH /auditoria/anomalias/{id}/status`):** exige o
  papel `SINDICO` ou `ADMIN`. Mudar o status de uma anomalia — marcar um alerta
  de seguranca como reconhecido — e uma acao humana de decisao, nao um fluxo
  automatico. Se qualquer usuario autenticado pudesse faze-lo, um morador poderia
  silenciar o alerta de que ele proprio falhou logins repetidamente, esvaziando o
  proposito do servico. Por isso essa rota especifica e mais restrita que as
  demais.

## Endpoints publicos e protegidos

| Acesso     | Endpoint                                      | Exige                       |
|------------|-----------------------------------------------|-----------------------------|
| Publico    | /actuator/health, /info, /metrics, /prometheus| nada                        |
| Protegido  | /auditoria/registros (GET, POST)              | autenticado (via Gateway)   |
| Protegido  | /auditoria/anomalias (GET)                    | autenticado (via Gateway)   |
| Protegido  | PATCH /auditoria/anomalias/{id}/status        | papel SINDICO ou ADMIN      |

## Exemplos de requisicao

Como a validacao do token ocorre no Gateway, os exemplos abaixo simulam o que o
Gateway injeta, batendo direto no servico com os headers de identidade. Rode a
partir da raiz do monorepo.

```bash
# 1. Sem identidade: rejeitado (403) -- nao passou pelo Gateway
curl.exe -s -o NUL -w "[HTTP %{http_code}]" http://localhost:8085/auditoria/registros

# 2. Autenticado (morador): consulta liberada (200)
curl.exe -s -o NUL -w "[HTTP %{http_code}]" -H "X-User-Id: user-123" -H "X-User-Roles: MORADOR" http://localhost:8085/auditoria/registros

# 3. Morador tentando triar anomalia: rejeitado (403) por falta de papel
curl.exe -s -o NUL -w "[HTTP %{http_code}]" -X PATCH -H "X-User-Id: user-123" -H "X-User-Roles: MORADOR" -H "Content-Type: application/json" -d "@auditoria_service/test-manual/status.json" http://localhost:8085/auditoria/anomalias/algum-id/status

# 4. Sindico triando: autorizacao aceita (chega na regra de negocio)
curl.exe -s -o NUL -w "[HTTP %{http_code}]" -X PATCH -H "X-User-Id: sindico-001" -H "X-User-Roles: SINDICO" -H "Content-Type: application/json" -d "@auditoria_service/test-manual/status.json" http://localhost:8085/auditoria/anomalias/algum-id/status
```

A sequencia de respostas (403, 200, 403, e aceito no caso 4) demonstra os dois
eixos: autenticacao (presenca da identidade do Gateway) e autorizacao (papel
necessario para acoes sensiveis).

## Limitacoes conscientes (TP3)

- **Os papeis dependem do contrato do Gateway:** os nomes `SINDICO`/`ADMIN`
  precisam casar com os papeis que o componente de autenticacao emite nos
  headers. Se o contrato do grupo usar outros nomes, basta ajustar as strings no
  SecurityConfig — a logica nao muda.
- **O servico confia no header sem reassinatura:** num ambiente real, garantir-se
  ia (via rede/mTLS) que so o Gateway pode injetar esses headers, para um cliente
  externo nao forja-los. No escopo do trabalho, com os servicos atras do Gateway,
  a confianca na borda e suficiente.