-- ═══════════════════════════════════════════════════════════
-- V1 — Schema inicial do portaria-service
-- Decisões:
--   • VARCHAR para enums → compatível com @Enumerated(EnumType.STRING) do JPA
--     (tipos ENUM nativos do PostgreSQL exigem cast e complicam migrações)
--   • pessoa_id único em registro_acesso → visitante_id removido conforme
--     decisão de modelagem (visitante.id é usado como pessoa_id quando tipo=VISITANTE)
--   • schema 'portaria' isolado dos outros serviços
-- ═══════════════════════════════════════════════════════════

CREATE SCHEMA IF NOT EXISTS portaria;

SET search_path TO portaria;

-- ───────────────────────────────────────────────────────────
-- VISITANTE
-- ───────────────────────────────────────────────────────────
CREATE TABLE visitante (
                           id                         UUID         PRIMARY KEY,
                           nome                       VARCHAR(200) NOT NULL,
                           documento                  VARCHAR(20),
                           telefone                   VARCHAR(20),
                           tipo                       VARCHAR(20)  NOT NULL,
                           autorizado_por_pessoa_id   UUID         NOT NULL,
                           autorizado_para_unidade_id UUID         NOT NULL,
                           validade_inicio            TIMESTAMP    NOT NULL,
                           validade_fim               TIMESTAMP    NOT NULL,
                           status                     VARCHAR(20)  NOT NULL DEFAULT 'AUTORIZADO',
                           criado_em                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT ck_visitante_validade CHECK (validade_fim > validade_inicio)
);

CREATE INDEX idx_visitante_documento        ON visitante(documento);
CREATE INDEX idx_visitante_unidade          ON visitante(autorizado_para_unidade_id);
CREATE INDEX idx_visitante_status_validade  ON visitante(status, validade_inicio, validade_fim);

-- ───────────────────────────────────────────────────────────
-- REGISTRO_ACESSO
-- pessoa_id cobre todos os tipos:
--   MORADOR / FUNCIONARIO / PRESTADOR → Pessoa.id do condominio-service
--   VISITANTE                         → Visitante.id deste serviço
-- ───────────────────────────────────────────────────────────
CREATE TABLE registro_acesso (
                                 id               UUID         PRIMARY KEY,
                                 tipo_pessoa      VARCHAR(20)  NOT NULL,
                                 pessoa_id        UUID         NOT NULL,
                                 unidade_id       UUID,
                                 veiculo_placa    VARCHAR(10),
                                 tipo_movimento   VARCHAR(10)  NOT NULL,
                                 timestamp_acesso TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 porteiro_id      UUID         NOT NULL,
                                 observacoes      VARCHAR(500)
);

CREATE INDEX idx_acesso_pessoa            ON registro_acesso(pessoa_id);
CREATE INDEX idx_acesso_unidade_timestamp ON registro_acesso(unidade_id, timestamp_acesso);
CREATE INDEX idx_acesso_timestamp         ON registro_acesso(timestamp_acesso);

-- ───────────────────────────────────────────────────────────
-- ENCOMENDA
-- ───────────────────────────────────────────────────────────
CREATE TABLE encomenda (
                           id                     UUID         PRIMARY KEY,
                           unidade_id             UUID         NOT NULL,
                           tipo                   VARCHAR(20)  NOT NULL,
                           descricao              VARCHAR(500),
                           codigo_rastreio        VARCHAR(100),
                           status                 VARCHAR(30)  NOT NULL DEFAULT 'AGUARDANDO_RETIRADA',
                           data_chegada           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           data_retirada          TIMESTAMP,
                           porteiro_recebedor_id  UUID         NOT NULL,
                           porteiro_entregador_id UUID,
                           retirado_por_pessoa_id UUID
);

CREATE INDEX idx_encomenda_unidade_status ON encomenda(unidade_id, status);
CREATE INDEX idx_encomenda_tipo_status    ON encomenda(tipo, status);