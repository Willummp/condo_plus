package com.condoplus.auditoria.anomalia;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.RegistroAuditoria;

import java.util.Optional;

/**
 * Contrato de uma regra de deteccao. Cada regra olha um evento e decide se
 * ele configura uma anomalia. Optional vazio = "nada a reportar".
 *
 * Adicionar uma regra = criar uma classe que implementa esta interface.
 * Nenhuma outra classe precisa mudar (Open/Closed).
 */
public interface RegraAnomalia {
    Optional<Anomalia> avaliar(RegistroAuditoria evento);
}