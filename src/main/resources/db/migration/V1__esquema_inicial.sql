SET search_path TO iam;
CREATE TABLE credencial_usuario (
                                    id UUID PRIMARY KEY,
                                    email VARCHAR(320) NOT NULL UNIQUE,
                                    senha_hash VARCHAR(60) NOT NULL,
                                    status VARCHAR(30) NOT NULL,
                                    tentativas_falhas INTEGER NOT NULL DEFAULT 0,
                                    bloqueado_ate TIMESTAMP,
                                    ultimo_login TIMESTAMP,
                                    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_credencial_email ON credencial_usuario(email);
CREATE TABLE role (
                      id BIGINT PRIMARY KEY,
                      nome VARCHAR(30) NOT NULL UNIQUE,
                      descricao VARCHAR(200)
);

INSERT INTO role (id, nome, descricao) VALUES
                                           (1, 'ADMIN', 'Administrador técnico do sistema'),
                                           (2, 'SINDICO', 'Gestor de negócio do condomínio'),
                                           (3, 'PORTEIRO', 'Operador de portaria'),
                                           (4, 'MORADOR', 'Usuário final residente ou proprietário'),
                                           (5, 'FUNCIONARIO', 'Staff operacional não-porteiro');
CREATE TABLE credencial_role (
                                 credencial_id UUID NOT NULL REFERENCES credencial_usuario(id) ON DELETE CASCADE,
                                 role_id BIGINT NOT NULL REFERENCES role(id),
                                 PRIMARY KEY (credencial_id, role_id)
);
CREATE INDEX idx_credencial_role_credencial ON credencial_role(credencial_id);