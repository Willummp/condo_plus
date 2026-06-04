package com.condoplus.condominio.estrutura.domain;

/**
 * Enumeração representando as possíveis relações ou naturezas jurídicas/sociais do vínculo de uma pessoa com uma unidade.
 */
public enum TipoVinculacao {

    /**
     * Dono legal da unidade residencial que não reside no condomínio (ex: investidores).
     */
    PROPRIETARIO,

    /**
     * Residente/morador inquilino da unidade residencial que usufrui da posse direta.
     */
    RESIDENTE,

    /**
     * Dono legal da unidade residencial que também reside e habita no próprio imóvel.
     */
    PROPRIETARIO_RESIDENTE
}
