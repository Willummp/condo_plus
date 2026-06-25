-- V4: torna nullable colunas que referenciam Pessoa como autor/aplicador
-- Motivo: ADMIN do sistema nao possui registro de Pessoa no condominio-service
-- (foi criado apenas via seed no iam-service). As colunas passam a aceitar NULL
-- para que ADMIN e SINDICO sem vinculacao possam aplicar multas e publicar comunicados.
ALTER TABLE condominio.multa
    ALTER COLUMN aplicada_por_id DROP NOT NULL;

ALTER TABLE condominio.comunicado
    ALTER COLUMN autor_id DROP NOT NULL;
