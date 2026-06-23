-- Seed do primeiro administrador do sistema (Admin@1234, BCrypt strength 12).
-- Idempotente: ON CONFLICT DO NOTHING garante que re-execucoes nao falhem.
INSERT INTO credencial_usuario (id, email, senha_hash, status, tentativas_falhas, criado_em)
VALUES (
    '1b0743f7-1b2b-4e9d-80dd-e82fddf68555',
    'admin@condoplus.com',
    '$2a$12$cdXqLZZ5sLhay0pbyTV6Z.Rz19RghNvUh9AMELGN0GQIyH10dAMGC',
    'ATIVO',
    0,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO credencial_role (credencial_id, role_id)
VALUES ('1b0743f7-1b2b-4e9d-80dd-e82fddf68555', 1)
ON CONFLICT DO NOTHING;
