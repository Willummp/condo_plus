SET search_path TO notificacao;

CREATE TABLE preferencia_notificacao (
    id UUID PRIMARY KEY,
    pessoa_id UUID NOT NULL,
    tipo_evento VARCHAR(40) NOT NULL,
    canal VARCHAR(20) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_preferencia UNIQUE (pessoa_id, tipo_evento, canal)
);

CREATE INDEX idx_preferencia_pessoa ON preferencia_notificacao(pessoa_id);
CREATE INDEX idx_preferencia_evento ON preferencia_notificacao(tipo_evento);

CREATE TABLE notificacao (
id UUID PRIMARY KEY,
destinatario_pessoa_id UUID NOT NULL,
tipo_evento VARCHAR(40) NOT NULL,
evento_origem_id VARCHAR(80) NOT NULL,
canal VARCHAR(20) NOT NULL,
titulo VARCHAR(200) NOT NULL,
corpo TEXT NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
tentativas INTEGER NOT NULL DEFAULT 0,
ultimo_erro TEXT,
criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
enviada_em TIMESTAMP,
CONSTRAINT uk_notif_idempotencia UNIQUE (destinatario_pessoa_id, evento_origem_id, canal)
);

CREATE INDEX idx_notif_destinatario ON notificacao(destinatario_pessoa_id);
CREATE INDEX idx_notif_status ON notificacao(status);
CREATE INDEX idx_notif_evento_origem ON notificacao(evento_origem_id);
CREATE INDEX idx_notif_criada_em ON notificacao(criada_em);
