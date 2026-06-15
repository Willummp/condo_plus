# 🏢 condominio-service — Microsserviço de Estrutura e Convivência

Este microsserviço é o núcleo de regras de negócios do **Condo+**, sendo responsável pelo gerenciamento cadastral de unidades residenciais, moradores, funcionários e veículos, além do controle de convivência (comunicados, multas e reservas de áreas comuns).

---

## ⚡ Arquitetura de Segurança (TP3)

Para atender as diretrizes de segurança do **TP3**, o ecossistema utiliza um modelo de **Autenticação Centralizada na Borda com Relay de Identidade (Gateway-based Token Validation & Relay)**.

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

1. **Validação na Borda (Bypass)**: O `api-gateway` intercepta todas as chamadas para as rotas do condomínio (`/api/condominio/**`). Ele valida a assinatura criptográfica e a expiração do token JWT recebido no cabeçalho `Authorization: Bearer <token>`.
2. **Propagação via Headers**: Se o token for válido, o Gateway descriptografa os claims e repassa a identidade do usuário para o `condominio-service` nos headers:
   * `X-User-Id` — UUID da credencial do usuário.
   * `X-User-Email` — Email associado à conta.
   * `X-User-Roles` — Perfis de permissão separados por vírgula (ex: `SINDICO,MORADOR`).
3. **Autorização Fina (Spring Security)**: O `condominio-service` consome os headers através do filtro `HeaderAuthenticationFilter` (configurado em [SecurityConfig.java](src/main/java/com/condoplus/condominio/config/SecurityConfig.java)) e injeta o objeto de autenticação no `SecurityContextHolder`. Isso permite o uso direto de regras a nível de método:
   * Exemplo: `@PreAuthorize("hasRole('SINDICO')")` limita ações de cadastro de multas apenas ao síndico.

---

## 🚦 Endpoints e Regras de Acesso

Todos os endpoints expostos no Gateway (`http://localhost:8080/api/condominio/**`) são protegidos e exigem cabeçalho de autenticação:

| Recurso (Gateway) | Método | Role Exigida | Descrição |
|---|---|---|---|
| `/api/condominio/unidades` | `POST` | `SINDICO`, `ADMIN` | Cadastro de nova unidade |
| `/api/condominio/unidades/{id}` | `GET` | Qualquer logado | Detalhamento de unidade |
| `/api/condominio/unidades/{id}/vinculacoes` | `POST` | `SINDICO`, `ADMIN` | Vincular pessoa a unidade |
| `/api/condominio/comunicados` | `POST` | `SINDICO` | Publicação de comunicado geral |
| `/api/condominio/unidades/{id}/multas` | `POST` | `SINDICO` | Aplicação de multa para a unidade |
| `/api/condominio/reservas` | `POST` | Qualquer logado | Criar reserva de área comum |

---

## 🏃 Como Executar e Testar o Serviço

### Pré-requisitos
* Java 21+ instalado.
* Docker rodando localmente.

### Passo a Passo
1. **Subir Banco de Dados e Kafka**:
   ```bash
   docker compose -f compose/docker-compose.yml up -d
   ```
2. **Executar dependências de Infra**:
   - Inicie o `eureka-server` (porta `8761`)
   - Inicie o `api-gateway` (porta `8080`)
   - Inicie o `iam-service` (porta `8081`, responsável por autenticar e prover tokens).
3. **Iniciar o Condominio Service**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

---

## 🧪 Roteiro de Testes Práticos (curl)

Abaixo estão os cenários prontos de simulação para a apresentação de avaliação prática do TP3:

### 1. Tentativa de Acesso sem Autenticação
Tente chamar a API de unidades sem enviar cabeçalho de segurança:
```bash
curl -i -X GET http://localhost:8080/api/condominio/unidades
```
* **Resposta Esperada**: `HTTP/1.1 401 Unauthorized` (Barreado na borda pelo API Gateway).

### 2. Autenticação e Obtenção de Token
Solicite a autenticação para o IAM Service passando credenciais válidas:
```bash
curl -i -X POST http://localhost:8080/api/iam/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "carlos@email.com", "senha": "senhaForte123"}'
```
* **Resposta Esperada**: Um JSON contendo o `accessToken` e o `refreshToken`.

### 3. Acesso Autorizado com Token Válido
Use o `accessToken` obtido no login no cabeçalho `Authorization`:
```bash
curl -i -X GET http://localhost:8080/api/condominio/unidades \
  -H "Authorization: Bearer <INSIRA_O_ACCESS_TOKEN_AQUI>"
```
* **Resposta Esperada**: `HTTP/1.1 200 OK` acompanhado da listagem JSON de unidades.

### 4. Renovação do Token (Refresh Flow)
Quando o token de acesso expirar, solicite um novo informando o token de refresh:
```bash
curl -i -X POST http://localhost:8080/api/iam/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<INSIRA_O_REFRESH_TOKEN_AQUI>"}'
```
* **Resposta Esperada**: Um novo `accessToken` e `refreshToken` válidos.

### 5. Rejeição de Token Inválido ou Adulterado
Tente fazer chamadas com tokens corrompidos ou alterados:
```bash
curl -i -X GET http://localhost:8080/api/condominio/unidades \
  -H "Authorization: Bearer tokenCorrompidoOuIncorreto"
```
* **Resposta Esperada**: `HTTP/1.1 401 Unauthorized` com o header `X-Error-Reason: Token inválido ou expirado`.
