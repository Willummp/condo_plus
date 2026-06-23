-- V2__convivencia_inicial.sql
-- Migration do sub-domínio Convivência:
-- Comunicado, Multa, AreaComum, Reserva

SET search_path TO condominio;

-- ─────────────────────────────────────────────────────────────────
-- COMUNICADO
-- Publicado pelo síndico para grupos de moradores.
-- bloco_alvo é nullable — usado só quando publico_alvo = BLOCO_ESPECIFICO
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE comunicado (
    id               UUID         PRIMARY KEY,
    titulo           VARCHAR(200) NOT NULL,
    mensagem         TEXT         NOT NULL,
    data_publicacao  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    autor_id         UUID         NOT NULL REFERENCES pessoa(id),
    publico_alvo     VARCHAR(30)  NOT NULL CHECK (publico_alvo IN ('TODOS', 'PROPRIETARIOS', 'RESIDENTES', 'BLOCO_ESPECIFICO')),
    bloco_alvo       VARCHAR(20)            -- null quando publico_alvo != BLOCO_ESPECIFICO
);

CREATE INDEX idx_comunicado_data        ON comunicado(data_publicacao DESC);
CREATE INDEX idx_comunicado_publico_alvo ON comunicado(publico_alvo);

-- ─────────────────────────────────────────────────────────────────
-- MULTA
-- Aplicada pelo síndico a uma unidade.
-- CONVIVENCIA: responsabilidade de quem tem escopo SOCIAL (morador).
-- ESTRUTURAL: responsabilidade de quem tem escopo LEGAL (proprietário).
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE multa (
    id                  UUID            PRIMARY KEY,
    unidade_id          UUID            NOT NULL REFERENCES unidade(id),
    valor               NUMERIC(10, 2)  NOT NULL CHECK (valor > 0),
    motivo              VARCHAR(500)    NOT NULL,
    categoria           VARCHAR(20)     NOT NULL CHECK (categoria IN ('CONVIVENCIA', 'ESTRUTURAL')),
    anexo_evidencia_url VARCHAR(500),
    data_aplicacao      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_vencimento     DATE            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PAGA', 'RECORRIDA')),
    aplicada_por_id     UUID            NOT NULL REFERENCES pessoa(id)
);

CREATE INDEX idx_multa_unidade   ON multa(unidade_id);
CREATE INDEX idx_multa_status    ON multa(status);
CREATE INDEX idx_multa_vencimento ON multa(data_vencimento);

-- ─────────────────────────────────────────────────────────────────
-- AREA_COMUM
-- Espaços compartilhados: piscina, churrasqueira, academia, etc.
-- nome é UNIQUE — não podem existir duas áreas com o mesmo nome.
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE area_comum (
    id                UUID           PRIMARY KEY,
    nome              VARCHAR(100)   NOT NULL UNIQUE,
    capacidade_maxima INTEGER        NOT NULL CHECK (capacidade_maxima > 0),
    valor_reserva     NUMERIC(10, 2),         -- pode ser gratuita (null)
    regras            TEXT,                   -- texto livre com regras de uso
    ativa             BOOLEAN        NOT NULL DEFAULT TRUE
);

-- ─────────────────────────────────────────────────────────────────
-- RESERVA
-- Solicitação de uso exclusivo de área comum por um morador.
-- CONSTRAINT ck_horario_valido: proteção no banco (além da validação na app).
--
-- ÍNDICE idx_reserva_area_data: acelera a query de detecção de conflito
-- (a mais executada deste serviço) — crucial para performance.
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE reserva (
    id            UUID       PRIMARY KEY,
    area_comum_id UUID       NOT NULL REFERENCES area_comum(id),
    morador_id    UUID       NOT NULL REFERENCES pessoa(id),
    data_reserva  DATE       NOT NULL,
    hora_inicio   TIME       NOT NULL,
    hora_fim      TIME       NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA' CHECK (status IN ('CONFIRMADA', 'CANCELADA', 'REALIZADA')),
    criada_em     TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_horario_valido CHECK (hora_fim > hora_inicio)
);

-- Índice parcial — só reservas CONFIRMADAS participam do check de conflito
CREATE INDEX idx_reserva_area_data ON reserva(area_comum_id, data_reserva)
    WHERE status = 'CONFIRMADA';

CREATE INDEX idx_reserva_morador ON reserva(morador_id);
