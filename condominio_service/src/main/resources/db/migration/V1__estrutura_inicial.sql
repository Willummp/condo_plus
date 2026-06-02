-- V1__estrutura_inicial.sql
-- Migration do sub-domínio Estrutura:
-- Pessoa, Unidade, Vinculacao, VinculacaoEscopo, Veiculo, Funcionario

SET search_path TO condominio;

-- ─────────────────────────────────────────────────────────────────
-- PESSOA
-- Identidade estável da pessoa física.
-- Existe antes da Credencial (iam-service) e persiste após desligamento.
-- credencial_id é referência EXTERNA ao iam-service (não AggregateReference).
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE pessoa (
    id            UUID        PRIMARY KEY,
    credencial_id UUID        NOT NULL UNIQUE,  -- referência ao iam-service
    nome_completo VARCHAR(200) NOT NULL,
    telefone      VARCHAR(30),
    documento     VARCHAR(20) UNIQUE,           -- CPF: apenas dígitos
    email_contato VARCHAR(320),
    criada_em     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pessoa_credencial ON pessoa(credencial_id);
CREATE INDEX idx_pessoa_documento  ON pessoa(documento);

-- ─────────────────────────────────────────────────────────────────
-- UNIDADE (Aggregate Root)
-- Apartamento ou casa no condomínio.
-- Existe independentemente de pessoas — pode estar vazia.
-- versao: campo @Version para controle de concorrência otimista.
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE unidade (
    id        UUID        PRIMARY KEY,
    numero    VARCHAR(20) NOT NULL,
    bloco     VARCHAR(20),
    tipo      VARCHAR(20) NOT NULL CHECK (tipo IN ('APARTAMENTO', 'CASA')),
    criada_em TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_unidade_numero_bloco UNIQUE (numero, bloco)
);

CREATE INDEX idx_unidade_bloco ON unidade(bloco);

-- ─────────────────────────────────────────────────────────────────
-- VINCULACAO (entidade interna do aggregate Unidade)
-- Relacionamento entre Pessoa e Unidade com tipo e status.
-- NÃO tem repositório próprio — gerenciada pelo UnidadeRepository.
-- ON DELETE CASCADE: vinculações são deletadas com a unidade.
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE vinculacao (
    id          UUID       PRIMARY KEY,
    unidade_id  UUID       NOT NULL REFERENCES unidade(id) ON DELETE CASCADE,
    pessoa_id   UUID       NOT NULL REFERENCES pessoa(id),
    tipo        VARCHAR(30) NOT NULL CHECK (tipo IN ('PROPRIETARIO', 'RESIDENTE', 'PROPRIETARIO_RESIDENTE')),
    data_inicio DATE        NOT NULL,
    data_fim    DATE,                              -- null = vinculação ativa
    status      VARCHAR(20) NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA', 'ENCERRADA'))
);

CREATE INDEX idx_vinculacao_unidade ON vinculacao(unidade_id);
CREATE INDEX idx_vinculacao_pessoa  ON vinculacao(pessoa_id);

-- ─────────────────────────────────────────────────────────────────
-- VINCULACAO_ESCOPO (entidade interna de Vinculacao)
-- Escopos derivados: SOCIAL, LEGAL, FINANCEIRO.
-- Calculados pelo EscopoDerivacaoService — nunca definidos manualmente.
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE vinculacao_escopo (
    vinculacao_id UUID       NOT NULL REFERENCES vinculacao(id) ON DELETE CASCADE,
    escopo        VARCHAR(20) NOT NULL CHECK (escopo IN ('SOCIAL', 'LEGAL', 'FINANCEIRO')),
    PRIMARY KEY (vinculacao_id, escopo)
);

-- ─────────────────────────────────────────────────────────────────
-- VEICULO
-- Aggregate Root próprio (tem repositório).
-- Referencia Unidade por ID (AggregateReference<Unidade, UUID>).
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE veiculo (
    id         UUID        PRIMARY KEY,
    placa      VARCHAR(10) NOT NULL UNIQUE,
    modelo     VARCHAR(100) NOT NULL,
    cor        VARCHAR(30),
    unidade_id UUID        NOT NULL REFERENCES unidade(id),
    ativo      BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_veiculo_unidade ON veiculo(unidade_id);
CREATE INDEX idx_veiculo_placa   ON veiculo(placa);

-- ─────────────────────────────────────────────────────────────────
-- FUNCIONARIO
-- Aggregate Root próprio.
-- Referencia Pessoa por ID — nunca deleta (soft delete via data_desligamento).
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE funcionario (
    id                 UUID       PRIMARY KEY,
    pessoa_id          UUID       NOT NULL UNIQUE REFERENCES pessoa(id),
    cargo              VARCHAR(30) NOT NULL CHECK (cargo IN ('PORTEIRO', 'JARDINEIRO', 'LIMPEZA', 'ADMINISTRATIVO')),
    data_admissao      DATE        NOT NULL,
    data_desligamento  DATE,                       -- null = funcionário ativo
    ativo              BOOLEAN    NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_funcionario_cargo ON funcionario(cargo, ativo);
