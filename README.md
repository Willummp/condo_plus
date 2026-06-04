# 🏢 Condo+ — Sistema de Gestão de Condomínios Residenciais

## 👥 Integrantes e Organização da Equipe
* **Turma:** Terça e Quinta
* **Tipo de Entrega:** Grupo (5 integrantes)
* **Organizador da Entrega:** Lucas Ferreira

### Divisão de Responsabilidades por Microsserviço:
| Aluno | Microsserviço sob Responsabilidade | Papel / Responsabilidade Adicional |
|---|---|---|
| **Lucas Ferreira** | `condominio-service` | Desenvolvimento do Core de Estrutura, Convivência e Testes de Integração |
| **Integrante 2** | `iam-service` | Gerenciamento de Credenciais de Acesso e Autenticação (Spring Security) |
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
                      [ Cliente (App/Web) ]
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
 (Schema condo)  (Schema iam)    (Schema port)   (Schema notif)
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

## 💾 Persistência de Dados e Banco Não Relacional (NoSQL)
Cada microsserviço gerencia seus próprios dados de forma isolada através de databases ou schemas PostgreSQL separados (`condominio`, `iam`, `portaria`).

### 🍃 Análise de Uso de Banco Não Relacional (NoSQL)
Como proposta de evolução para persistência poliglota na arquitetura Condo+, identificou-se que o microsserviço de suporte **`auditoria-service`** seria um excelente candidato para a adoção de um banco de dados não relacional (NoSQL), como o **MongoDB**:
* **Estrutura Flexível de Documentos:** Os logs de auditoria possuem dados de naturezas muito diferentes dependendo da operação realizada. Um banco NoSQL de documentos permite armazenar esses payloads flexíveis no formato JSON sem a necessidade de migrações estruturais rígidas de tabelas.
* **Escrita de Alto Desempenho:** O serviço de auditoria é extremamente intensivo em operações de gravação (Write-Heavy) e não necessita de transações relacionais complexas (Joins/ACID), o que se alinha perfeitamente com os pontos fortes do MongoDB.

---

## ⚡ Estratégia de Resiliência
Para evitar falhas em cascata na chamada síncrona do `condominio-service` para o `iam-service` (cadastro de credenciais), configuramos o **Resilience4j**:
* **Circuit Breaker:** Abre o circuito se mais de 50% das últimas 10 chamadas falharem ou estourarem o timeout, redirecionando o fluxo para um método de fallback amigável.
* **Time Limiter:** Aplica um timeout de 3 segundos nas chamadas ao IAM, impedindo que requisições presas esgotem os recursos do servidor Tomcat local.

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

## 📡 Exemplos de Requisições

### 1. Criar uma nova Unidade (via Gateway)
* **Rota:** `POST http://localhost:8080/api/condominio/unidades`
* **Body:**
  ```json
  {
    "numero": "202",
    "bloco": "B",
    "tipo": "APARTAMENTO"
  }
  ```

### 2. Cadastrar uma Pessoa e solicitar credencial (integração síncrona com IAM via Gateway)
* **Rota:** `POST http://localhost:8080/api/condominio/pessoas`
* **Body:**
  ```json
  {
    "nomeCompleto": "Carlos Alberto",
    "email": "carlos@email.com",
    "senhaInicial": "senhaForte123",
    "documento": "98765432100",
    "telefone": "21988887777",
    "role": "MORADOR"
  }
  ```