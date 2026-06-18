package com.condoplus.auditoria.dto;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.SeveridadeAnomalia;
import com.condoplus.auditoria.domain.StatusAnomalia;

import java.time.Instant;

/**
 * DTO de saida das anomalias. Expoe so o que o sindico precisa ver,
 * sem vazar a entidade de dominio direto na API.
 */
public record AnomaliaResponse(
        String id,
        String tipoRegra,
        SeveridadeAnomalia severidade,
        String descricao,
        String entidadeRelacionada,
        String correlationId,
        String eventoGatilhoId,
        StatusAnomalia status,
        Instant detectadaEm
) {
    public static AnomaliaResponse de(Anomalia a) {
        return new AnomaliaResponse(
                a.getId(), a.getTipoRegra(), a.getSeveridade(), a.getDescricao(),
                a.getEntidadeRelacionada(), a.getCorrelationId(), a.getEventoGatilhoId(),
                a.getStatus(), a.getDetectadaEm());
    }
}