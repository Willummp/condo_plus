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