package com.condoplus.condominio.estrutura.domain;

/**
 * Enumeração representando os escopos de privilégios e responsabilidades de uma vinculação.
 * 
 * <p>Estes escopos determinam as permissões e ações permitidas para cada tipo de vínculo 
 * nas diferentes rotas de negócio e integrações da plataforma Condo Plus.
 */
public enum Escopo {

    /**
     * Privilégios sociais e cotidianos.
     * 
     * <p>Permissões incluídas:
     * <ul>
     *   <li>Autorização de entrada para visitantes e prestadores de serviço.</li>
     *   <li>Realização de reservas de áreas comuns (salão de festas, churrasqueiras).</li>
     *   <li>Recebimento de comunicados e avisos murais do condomínio.</li>
     * </ul>
     */
    SOCIAL,

    /**
     * Privilégios legais e deliberativos sobre o imóvel.
     * 
     * <p>Permissões incluídas:
     * <ul>
     *   <li>Votação em assembleias gerais ordinárias ou extraordinárias.</li>
     *   <li>Assinatura de contratos, autorização de obras na unidade residencial.</li>
     *   <li>Recebimento e contestação formal de multas de infração estrutural.</li>
     * </ul>
     */
    LEGAL,

    /**
     * Privilégios e responsabilidades financeiras da unidade.
     * 
     * <p>Permissões incluídas:
     * <ul>
     *   <li>Acesso a boletos bancários da taxa condominial mensal.</li>
     *   <li>Responsabilidade pelo pagamento de taxas extras e acordos de inadimplência.</li>
     * </ul>
     */
    FINANCEIRO
}
