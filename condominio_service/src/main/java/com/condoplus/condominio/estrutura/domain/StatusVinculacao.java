package com.condoplus.condominio.estrutura.domain;

/**
 * Enumeração representando os possíveis estados de um vínculo residencial.
 */
public enum StatusVinculacao {

    /**
     * Vínculo ativo e vigente, concedendo privilégios imediatos à pessoa na unidade.
     */
    ATIVA,

    /**
     * Vínculo inativo ou expirado (histórico), revogando qualquer privilégio de acesso.
     */
    ENCERRADA
}
