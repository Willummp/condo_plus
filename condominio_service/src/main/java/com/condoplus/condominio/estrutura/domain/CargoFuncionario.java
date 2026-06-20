package com.condoplus.condominio.estrutura.domain;

/**
 * Enumeração contendo os cargos trabalhistas e operacionais reconhecidos no condomínio.
 * 
 * <p>Utilizado para classificar a função de um funcionário cadastrado no microserviço.
 */
public enum CargoFuncionario {

    /**
     * Profissional responsável pelo controle de acessos físico na portaria do condomínio.
     */
    PORTEIRO,

    /**
     * Profissional responsável pela manutenção e conservação das áreas verdes comuns.
     */
    JARDINEIRO,

    /**
     * Profissional responsável pela higiene e limpeza das áreas sociais e de circulação.
     */
    LIMPEZA,

    /**
     * Profissional responsável pelas funções burocráticas e suporte administrativo da gestão do condomínio.
     */
    ADMINISTRATIVO
}
