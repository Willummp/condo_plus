package com.condoplus.condominio.convivencia.domain;

/**
 * Enumeração representando o público-alvo ou segmentação de visibilidade para um comunicado.
 * 
 * <p>Usado no mural para restringir quem tem direito de ler ou receber as notificações do aviso.
 */
public enum PublicoAlvo {

    /**
     * O comunicado é visível para qualquer pessoa associada ao condomínio.
     */
    TODOS,

    /**
     * Comunicado restrito apenas a proprietários (residente ou não).
     */
    PROPRIETARIOS,

    /**
     * Comunicado restrito apenas a moradores residentes (inquilinos ou proprietários moradores).
     */
    RESIDENTES,

    /**
     * Comunicado direcionado e restrito aos residentes de um bloco residencial específico.
     * 
     * <p>Nota: Exige obrigatoriamente que o campo {@code blocoAlvo} esteja preenchido.
     */
    BLOCO_ESPECIFICO
}
