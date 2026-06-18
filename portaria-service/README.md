# portaria-service

Serviço responsável pela operação da portaria do condomínio. Gerencia visitantes autorizados, registros de acesso e encomendas recebidas.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Web | Spring MVC |
| Persistência principal | JPA + PostgreSQL |
| Persistência TTL | Redis (encomendas CURTO_PRAZO) |
| Mensageria | Kafka (TP2) |
| Service discovery | Eureka |
| Circuit Breaker | Resilience4j |
| Migrations | Flyway |

---

## Configuração

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil ativo (`dev` ou `docker`) |
| `PORTARIA_DB_URL` | `jdbc:postgresql://localhost:5432/condoplus` | URL do banco |
| `PORTARIA_DB_USER` | `postgres` | Usuário do banco |
| `PORTARIA_DB_PASSWORD` | `postgres` | Senha do banco |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `EUREKA_URL` | `http://localhost:8761/eureka` | URL do Eureka |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Bootstrap servers do Kafka |

### Porta padrão

```
8083
```

### Schema do banco

O Flyway cria e gerencia o schema `portaria` automaticamente na inicialização.

---

## Autenticação

O serviço não valida JWT diretamente. Espera os headers propagados pelo **API Gateway**:

| Header | Exemplo | Descrição |
|---|---|---|
| `X-User-Id` | `uuid-da-pessoa` | UUID da Pessoa autenticada — vira o `@AuthenticationPrincipal` |
| `X-User-Roles` | `ROLE_PORTEIRO,ROLE_MORADOR` | Roles separadas por vírgula |

Roles disponíveis: `ROLE_PORTEIRO`, `ROLE_MORADOR`, `ROLE_SINDICO`, `ROLE_ADMIN`, `ROLE_FUNCIONARIO`.

---

## Rotas

### Visitantes — `/portaria/visitantes`

#### `POST /portaria/visitantes`
Autoriza um visitante para uma unidade.

- **Role mínima:** qualquer autenticado
- **Regra:** `tipo: PRESTADOR` exige `ROLE_SINDICO` ou `ROLE_ADMIN`

**Request:**
```json
{
  "nome": "Ana Convidada",
  "documento": "11122233344",
  "telefone": "21999990001",
  "tipo": "SOCIAL",
  "unidadeId": "uuid-da-unidade",
  "validadeInicio": "2025-12-01T09:00:00",
  "validadeFim": "2025-12-01T22:00:00"
}
```

Valores de `tipo`: `SOCIAL`, `PRESTADOR`

**Response `201`:**
```json
{
  "id": "uuid",
  "nome": "Ana Convidada",
  "documento": "11122233344",
  "tipo": "SOCIAL",
  "unidadeId": "uuid-da-unidade",
  "validadeInicio": "2025-12-01T09:00:00",
  "validadeFim": "2025-12-01T22:00:00",
  "status": "AUTORIZADO",
  "criadoEm": "2025-11-20T14:30:00"
}
```

**Erros:**
| Status | Motivo |
|---|---|
| `403` | PRESTADOR sendo autorizado por não-SÍNDICO |
| `400` | `validadeFim` anterior a `validadeInicio` |

---

#### `GET /portaria/visitantes?unidadeId={uuid}`
Lista todos os visitantes de uma unidade.

- **Role mínima:** qualquer autenticado

**Response `200`:** array de visitantes (mesmo formato do POST).

---

#### `PATCH /portaria/visitantes/{id}/encerrar`
Encerra manualmente um visitante ativo.

- **Role mínima:** qualquer autenticado

**Response `204`:** sem corpo.

**Erros:**
| Status | Motivo |
|---|---|
| `404` | Visitante não encontrado |
| `422` | Visitante já encerrado ou bloqueado |

---

### Registro de Acesso — `/portaria/acessos`

#### `POST /portaria/acessos/entrada/morador`
Registra entrada de morador. Se `veiculoPlaca` informado, valida com o `condominio-service`.

- **Role obrigatória:** `ROLE_PORTEIRO`

**Request:**
```json
{
  "moradorId": "uuid-da-pessoa",
  "unidadeId": "uuid-da-unidade",
  "veiculoPlaca": "ABC1D23",
  "observacoes": "Entrada noturna"
}
```

`veiculoPlaca` deve ter exatamente 7 caracteres alfanuméricos (`[A-Z0-9]{7}`). Campos opcionais: `veiculoPlaca`, `observacoes`.

**Response `201`:**
```json
{
  "id": "uuid",
  "tipoPessoa": "MORADOR",
  "pessoaId": "uuid-da-pessoa",
  "unidadeId": "uuid-da-unidade",
  "veiculoPlaca": "ABC1D23",
  "tipoMovimento": "ENTRADA",
  "timestamp": "2025-11-20T14:35:00",
  "porteiroId": "uuid-do-porteiro"
}
```

**Erros:**
| Status | Motivo |
|---|---|
| `400` | Placa com formato inválido |
| `422` | Placa não cadastrada no sistema |
| `422` | Placa não pertence à unidade informada |
| `503` | `condominio-service` indisponível (Circuit Breaker aberto) — entrada permitida em modo degradado |

---

#### `POST /portaria/acessos/entrada/visitante`
Registra entrada de visitante. Valida se está autorizado e dentro da janela de validade.

- **Role obrigatória:** `ROLE_PORTEIRO`

**Request:**
```json
{
  "visitanteId": "uuid-do-visitante",
  "observacoes": null
}
```

**Response `201`:** mesmo formato do acesso de morador.

**Erros:**
| Status | Motivo |
|---|---|
| `404` | Visitante não encontrado |
| `422` | Visitante fora da janela de validade ou bloqueado |

---

#### `POST /portaria/acessos/saida`
Registra saída de qualquer tipo de pessoa.

- **Role obrigatória:** `ROLE_PORTEIRO`

**Request:**
```json
{
  "tipoPessoa": "MORADOR",
  "pessoaId": "uuid-da-pessoa",
  "unidadeId": "uuid-da-unidade",
  "veiculoPlaca": null,
  "observacoes": null
}
```

Valores de `tipoPessoa`: `MORADOR`, `VISITANTE`, `FUNCIONARIO`, `PRESTADOR`.

**Response `201`:** mesmo formato dos outros acessos.

---

### Encomendas — `/portaria/encomendas`

#### `POST /portaria/encomendas`
Registra chegada de encomenda. Encomendas `CURTO_PRAZO` recebem TTL de 2h no Redis.

- **Role obrigatória:** `ROLE_PORTEIRO`

**Request:**
```json
{
  "unidadeId": "uuid-da-unidade",
  "tipo": "CURTO_PRAZO",
  "descricao": "Pizza iFood - 2 caixas",
  "codigoRastreio": null
}
```

Valores de `tipo` e TTL:

| Tipo | TTL | Exemplo |
|---|---|---|
| `CURTO_PRAZO` | 2 horas | iFood, Rappi |
| `MEDIO_PRAZO` | 7 dias | Amazon, Magalu |
| `LONGO_PRAZO` | 30 dias | Shopee, Correios |

**Response `201`:**
```json
{
  "id": "uuid",
  "unidadeId": "uuid-da-unidade",
  "tipo": "CURTO_PRAZO",
  "descricao": "Pizza iFood - 2 caixas",
  "codigoRastreio": null,
  "status": "AGUARDANDO_RETIRADA",
  "dataChegada": "2025-11-20T14:40:00",
  "dataRetirada": null
}
```

---

#### `GET /portaria/encomendas?unidadeId={uuid}`
Lista encomendas com status `AGUARDANDO_RETIRADA` de uma unidade.

- **Role mínima:** qualquer autenticado

**Response `200`:** array de encomendas (mesmo formato do POST).

---

#### `GET /portaria/encomendas/{id}`
Busca encomenda por ID.

- **Role mínima:** qualquer autenticado

**Response `200`:** encomenda (mesmo formato do POST).

**Erros:**
| Status | Motivo |
|---|---|
| `404` | Encomenda não encontrada |

---

#### `POST /portaria/encomendas/{id}/retirada`
Registra retirada de encomenda pelo morador.

- **Role obrigatória:** `ROLE_PORTEIRO`

**Request:**
```json
{
  "retiradoPorPessoaId": "uuid-da-pessoa"
}
```

**Response `200`:** encomenda com `status: RETIRADA` e `dataRetirada` preenchida.

**Erros:**
| Status | Motivo |
|---|---|
| `404` | Encomenda não encontrada |
| `409` | Encomenda já retirada ou expirada |

---

## Eventos Kafka publicados (TP2)

| Tópico | Quando | Chave |
|---|---|---|
| `portaria.acessos` | Qualquer entrada ou saída registrada | `registroId` |
| `portaria.encomendas.recebidas` | Encomenda recebida | `unidadeId` |
| `portaria.encomendas.retiradas` | Encomenda retirada | `unidadeId` |

### Formato — `portaria.acessos`
```json
{
  "eventId": "uuid",
  "occurredAt": "2025-11-20T14:35:00Z",
  "registroId": "uuid",
  "tipoPessoa": "MORADOR",
  "pessoaId": "uuid",
  "unidadeId": "uuid",
  "tipoMovimento": "ENTRADA",
  "veiculoPlaca": "ABC1D23"
}
```

### Formato — `portaria.encomendas.recebidas`
```json
{
  "eventId": "uuid",
  "occurredAt": "2025-11-20T14:40:00Z",
  "encomendaId": "uuid",
  "unidadeId": "uuid",
  "tipo": "CURTO_PRAZO",
  "descricao": "Pizza iFood",
  "codigoRastreio": null
}
```

### Formato — `portaria.encomendas.retiradas`
```json
{
  "eventId": "uuid",
  "occurredAt": "2025-11-20T15:10:00Z",
  "encomendaId": "uuid",
  "unidadeId": "uuid",
  "retiradoPorPessoaId": "uuid",
  "dataRetirada": "2025-11-20T15:10:00"
}
```

## Evento Kafka consumido (TP2)

| Tópico | Producer | Ação |
|---|---|---|
| `credenciais.criadas` | `iam-service` | Log da credencial criada |

---

## Erros — formato padrão

Todos os erros seguem o padrão `ProblemDetail` (RFC 7807):

```json
{
  "type": "https://condoplus.local/errors/nao-encontrado",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Encomenda não encontrada: uuid"
}
```

Erros de validação incluem o campo `erros`:
```json
{
  "type": "https://condoplus.local/errors/validacao",
  "title": "Validação falhou",
  "status": 400,
  "detail": "Dados inválidos",
  "erros": {
    "nome": "must not be blank",
    "unidadeId": "must not be null"
  }
}
```

---

## Healthcheck

```
GET /actuator/health
GET /actuator/info
```

Disponíveis sem autenticação.