-- 🔹 Criar schema
CREATE SCHEMA IF NOT EXISTS portaria;

CREATE TYPE portaria.tipo_visitante AS ENUM ('SOCIAL', 'PRESTADOR');
CREATE TYPE portaria.status_visitante AS ENUM ('AUTORIZADO', 'ENCERRADO', 'BLOQUEADO');

CREATE TYPE portaria.tipo_pessoa_acesso AS ENUM ('MORADOR', 'VISITANTE', 'FUNCIONARIO', 'PRESTADOR');
CREATE TYPE portaria.tipo_movimento AS ENUM ('ENTRADA', 'SAIDA');

CREATE TYPE portaria.tipo_encomenda AS ENUM ('CURTO_PRAZO', 'MEDIO_PRAZO', 'LONGO_PRAZO');
CREATE TYPE portaria.status_encomenda AS ENUM ('AGUARDANDO_RETIRADA', 'RETIRADA', 'EXPIRADA');

CREATE TABLE portaria.visitante (
                                    id UUID PRIMARY KEY,

                                    nome VARCHAR(200) NOT NULL,
                                    documento VARCHAR(20),
                                    telefone VARCHAR(20),

                                    tipo portaria.tipo_visitante NOT NULL,

                                    autorizado_por_pessoa_id UUID NOT NULL,
                                    autorizado_para_unidade_id UUID NOT NULL,

                                    validade_inicio TIMESTAMP NOT NULL,
                                    validade_fim TIMESTAMP NOT NULL,

                                    status portaria.status_visitante NOT NULL,

                                    criado_em TIMESTAMP NOT NULL
);


CREATE TABLE portaria.registro_acesso (
                                          id UUID PRIMARY KEY,

                                          tipo_pessoa portaria.tipo_pessoa_acesso NOT NULL,

                                          pessoa_id UUID,
                                          visitante_id UUID,

                                          unidade_id UUID,
                                          veiculo_placa VARCHAR(10),

                                          tipo_movimento portaria.tipo_movimento NOT NULL,
                                          timestamp_acesso TIMESTAMP NOT NULL,

                                          porteiro_id UUID NOT NULL,

                                          observacoes VARCHAR(500)
);


CREATE TABLE portaria.encomenda (
                                    id UUID PRIMARY KEY,

                                    unidade_id UUID NOT NULL,

                                    tipo portaria.tipo_encomenda NOT NULL,

                                    descricao VARCHAR(500),
                                    codigo_rastreio VARCHAR(100),

                                    status portaria.status_encomenda NOT NULL,

                                    data_chegada TIMESTAMP NOT NULL,
                                    data_retirada TIMESTAMP,

                                    porteiro_recebedor_id UUID NOT NULL,
                                    porteiro_entregador_id UUID,
                                    retirado_por_pessoa_id UUID
);



CREATE INDEX idx_visitante_documento
    ON portaria.visitante(documento);

CREATE INDEX idx_visitante_unidade
    ON portaria.visitante(autorizado_para_unidade_id);

CREATE INDEX idx_registro_unidade_data
    ON portaria.registro_acesso(unidade_id, timestamp_acesso);

CREATE INDEX idx_registro_pessoa
    ON portaria.registro_acesso(pessoa_id);

CREATE INDEX idx_encomenda_unidade_status
    ON portaria.encomenda(unidade_id, status);

CREATE INDEX idx_encomenda_tipo_status
    ON portaria.encomenda(tipo, status);

ALTER TABLE portaria.registro_acesso
    ADD CONSTRAINT chk_pessoa_ou_visitante
        CHECK (
            (tipo_pessoa = 'VISITANTE' AND visitante_id IS NOT NULL AND pessoa_id IS NULL)
                OR
            (tipo_pessoa <> 'VISITANTE' AND pessoa_id IS NOT NULL AND visitante_id IS NULL)
            );