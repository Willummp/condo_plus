package com.condoplus.auditoria.domain;

/**
 * Tipos de eventos auditaveis no Condo+.
 *
 * Cada valor corresponde a um evento de dominio que algum servico publica
 * (no TP1 via REST direto, no TP2 via Kafka). O auditoria-service traduz
 * cada um para um RegistroAuditoria com estrutura comum.
 *
 * Por que enum e nao String livre:
 * - Compile-time safety: erro de digitacao detectado em desenvolvimento.
 * - Permite switch para roteamento por tipo no mapper (TP2).
 * - Persistido como String no Mongo (default do Spring Data MongoDB) —
 *   ou seja, no banco nao perdemos legibilidade, mas no codigo ganhamos
 *   o controle do compilador.
 *
 * Atende: RF-AUD-01 (categorizacao dos eventos auditaveis).
 */
public enum TipoEvento {

    // ===== IAM =====
    CREDENCIAL_CRIADA,
    CREDENCIAL_BLOQUEADA,
    CREDENCIAL_DESBLOQUEADA,
    LOGIN_REALIZADO,
    LOGIN_FALHADO,

    // ===== Condominio =====
    PESSOA_CADASTRADA,
    PESSOA_ATUALIZADA,
    VINCULACAO_CRIADA,
    VINCULACAO_ENCERRADA,
    MULTA_APLICADA,
    MULTA_QUITADA,
    RESERVA_CONFIRMADA,
    RESERVA_CANCELADA,
    COMUNICADO_PUBLICADO,

    // ===== Portaria =====
    ACESSO_REGISTRADO,
    VISITANTE_AUTORIZADO,
    ENCOMENDA_RECEBIDA,
    ENCOMENDA_RETIRADA,

    // ===== Fallback =====
    OUTRO // para eventos novos ainda nao mapeados
}