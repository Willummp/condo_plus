# TP3 — Autenticação e Autorização em Microservices

## 1. Objetivo

Implementar autenticação e autorização em uma arquitetura de microsserviços.

O sistema deverá possuir:
- Um microserviço **dedicado à autenticação**
- Pelo menos **um microserviço com rotas protegidas**

---

## 2. Requisitos Obrigatórios

### 2.1 Microsserviço de Autenticação

Um microserviço responsável pela autenticação dos usuários. Exemplos de nome: `auth-service`, `authentication-service`, `identity-service`.

Deve disponibilizar:

| Endpoint | Descrição |
|---|---|
| `POST /auth/login` | Autenticação do usuário |
| `POST /auth/refresh` | Renovação do token |

### 2.2 Tecnologia de Autenticação

Obrigatoriamente uma das opções abaixo:

- **JWT** (JSON Web Token)
- **Keycloak**

> Não serão aceitas outras formas de autenticação.

### 2.3 Proteção de Rotas

Pelo menos um outro microserviço com rotas protegidas. Exemplos:
- `GET /api/clientes`
- `POST /api/pedidos`
- `GET /api/containers`

O acesso deve exigir autenticação válida.

### 2.4 Fluxo Esperado

1. Usuário realiza autenticação
2. Sistema retorna credencial válida
3. Usuário utiliza a credencial para acessar recursos protegidos
4. Requisições **sem autenticação** devem ser **rejeitadas**
5. Requisições **autenticadas** devem ser processadas normalmente
6. Quando necessário, usuário usa o endpoint de refresh para obter novo token

---

## 3. Demonstração (avaliação)

Durante a avaliação deve ser demonstrado:

- [ ] Tentativa de acesso **sem** autenticação (deve ser rejeitada)
- [ ] Autenticação realizada com sucesso
- [ ] Obtenção do token
- [ ] Utilização do endpoint de **refresh**
- [ ] Obtenção de novo token via refresh
- [ ] Acesso a rota protegida utilizando o token
- [ ] Rejeição de credenciais inválidas

---

## 4. Documentação (README)

O README deve explicar:

- Arquitetura da solução
- Tecnologia escolhida (JWT ou Keycloak)
- Como executar os serviços
- Como realizar autenticação
- Como utilizar o endpoint de refresh
- Quais endpoints são **públicos**
- Quais endpoints são **protegidos**
- Exemplos de requisições para teste

---

## 5. Entregáveis

- Código-fonte completo
- Arquivo README
- Arquivos de configuração necessários para execução do ambiente

---

## 6. Observações

- O microserviço de autenticação deve ser **independente** dos demais serviços.
- O token deve ser enviado nas requisições para acesso aos recursos protegidos.
- A solução deve ser **executável e demonstrável** durante a apresentação.
- O mecanismo de autenticação deve utilizar **obrigatoriamente JWT ou Keycloak**.

---
---

# 🔐 TP3 — Autenticação e Autorização com JWT (API Gateway)

Este guia explica a arquitetura de segurança implementada no **Condo+** para o **TP3**, a tecnologia utilizada, a lista de endpoints públicos e protegidos, e como simular o fluxo de login, acesso protegido e refresh de tokens.

---

## 🗺️ Arquitetura da Solução

Implementamos um modelo de **Autenticação Centralizada na Borda (Gateway-based Token Validation & Relay)**.

```
       [ Cliente ]
            │
            ▼ (Requisição HTTP com Bearer Token)
     [ API Gateway ] ◄──── (Valida assinatura e expiração do JWT)
            │
            ├─► Se inválido: Retorna 401 Unauthorized
            │
            ▼ (Se válido: Injeta cabeçalhos X-User-Id e X-User-Roles)
[ condominio-service ] ◄─── (Autoriza via @PreAuthorize com Spring Security)
```

1. **API Gateway (`api-gateway`)**: Atua como o único ponto de entrada para rotas protegidas (`/api/condominio/**`). Ele possui um filtro personalizado (`JwtAuthenticationFilter`) que decodifica o JWT, valida a sua assinatura contra uma chave secreta e verifica a data de expiração.
2. **Propagação de Identidade**: Após a validação com sucesso, o gateway adiciona cabeçalhos HTTP (`X-User-Id`, `X-User-Email` e `X-User-Roles`) antes de encaminhar a requisição para o microsserviço correspondente.
3. **Autorização Downstream (`condominio-service`)**: O microsserviço de destino consome os cabeçalhos (`X-User-*`) e monta o contexto de segurança do Spring Security (`SecurityContextHolder`), permitindo a validação de roles por meio de `@PreAuthorize("hasRole('SINDICO')")`.
4. **Independência do Provedor de Identidade (`iam-service`)**: O serviço de IAM (desenvolvido de forma independente) é o responsável por expor os endpoints de geração de tokens (`/auth/login`) e renovação (`/auth/refresh`). Ambos os serviços compartilham apenas a chave de criptografia do token.

---

## 🛠️ Tecnologia Escolhida

* **JWT (JSON Web Token)**: Implementado utilizando a biblioteca estável `com.auth0:java-jwt` para codificação/decodificação com criptografia **HMAC-SHA256**.
* **Spring Cloud Gateway (Filtros Reativos)**: Utilizado para a interceptação não-bloqueante das requisições na camada de infraestrutura.

---

## 🚦 Endpoints do Ecossistema

### 🟢 Endpoints Públicos
Estes endpoints podem ser acessados diretamente através do Gateway sem a necessidade de passar um cabeçalho `Authorization`.

| Serviço | Rota Interna | Rota Externa (Gateway) | Método | Descrição |
|---|---|---|---|---|
| `iam-service` | `/auth/login` | `/api/iam/auth/login` | `POST` | Realiza autenticação e obtém tokens |
| `iam-service` | `/auth/refresh` | `/api/iam/auth/refresh` | `POST` | Renovação do token de acesso expirado |
| `eureka-server` | `/` | — | `GET` | Dashboard do Discovery Server |
| Todos | `/actuator/health`| — | `GET` | Verificação de integridade do serviço |

### 🔴 Endpoints Protegidos
Todos os endpoints do `condominio-service` são protegidos e exigem um Bearer Token válido no cabeçalho `Authorization`. A autorização fina de perfis é controlada individualmente:

| Rota Externa (Gateway) | Método | Role Exigida |
|---|---|---|
| `/api/condominio/unidades` | `POST` | `SINDICO`, `ADMIN` |
| `/api/condominio/unidades/{id}` | `GET` | Qualquer usuário autenticado |
| `/api/condominio/unidades/{id}/vinculacoes` | `POST` | `SINDICO`, `ADMIN` |
| `/api/condominio/comunicados` | `POST` | `SINDICO` |
| `/api/condominio/pessoas/{id}` | `GET` | Qualquer usuário autenticado |

---

## 🏃 Como Executar o Ambiente

1. **Suba o Banco e Kafka**:
   ```bash
   docker compose -f condominio_service/compose/docker-compose.yml up -d
   ```
2. **Suba o Eureka**:
   ```bash
   cd infra/eureka-server
   mvn spring-boot:run
   ```
3. **Suba o API Gateway**:
   ```bash
   cd infra/api-gateway
   mvn spring-boot:run
   ```
4. **Suba o Condominio Service**:
   ```bash
   cd condominio_service
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
5. **Suba o IAM Service** na porta `8081` (a partir de sua respectiva branch/diretório).

---

## 🧪 Exemplos de Requisições para Testes (curl)

Abaixo estão os comandos para demonstrar os fluxos exigidos na avaliação prática do TP3.

### 1. Tentativa de Acesso sem Autenticação
Tentar listar as unidades do condomínio sem enviar credenciais:
```bash
curl -i -X GET http://localhost:8080/api/condominio/unidades
```
**Resposta Esperada**: `HTTP/1.1 401 Unauthorized` (Rejeitado pelo Gateway).

---

### 2. Autenticação com Sucesso (Obtenção do Token)
Submeta o email e senha no endpoint de login:
```bash
curl -i -X POST http://localhost:8080/api/iam/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "carlos@email.com", "senha": "senhaForte123"}'
```
**Resposta Esperada**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1dWlkLXVzdWFyaW8iLCJlbWFpbCI6ImNhcmxvcyBAZW1haWwuY29tIiwicm9sZXMiOlsidXNlciJdLCJleHAiOjE2NzI1MzExMDB9...",
  "refreshToken": "7891-uuid-refresh-token..."
}
```

---

### 3. Acesso a Rota Protegida utilizando o Token Válido
Utilize o token recebido no cabeçalho `Authorization`:
```bash
curl -i -X GET http://localhost:8080/api/condominio/unidades \
  -H "Authorization: Bearer <INSIRA_O_ACCESS_TOKEN_AQUI>"
```
**Resposta Esperada**: `HTTP/1.1 200 OK` acompanhado da listagem JSON de unidades.

---

### 4. Utilização do Endpoint de Refresh
Quando o token de acesso expirar, solicite um novo informando o token de refresh obtido no login:
```bash
curl -i -X POST http://localhost:8080/api/iam/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<INSIRA_O_REFRESH_TOKEN_AQUI>"}'
```
**Resposta Esperada**: Um novo `accessToken` e um novo `refreshToken` válidos.

---

### 5. Rejeição de Credenciais Inválidas ou Expiradas
Se for enviado um token expirado, adulterado ou com senha incorreta:
```bash
curl -i -X GET http://localhost:8080/api/condominio/unidades \
  -H "Authorization: Bearer tokenTotalmenteIncorreto"
```
**Resposta Esperada**: `HTTP/1.1 401 Unauthorized` com o header `X-Error-Reason: Token inválido ou expirado`.
