package com.condoplus.auditoria.dto;

import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * DTO de saida. Expoe so o que interessa ao cliente, incluindo o id
 * gerado e a dataInsercao (quando o registro entrou no Mongo).
 */
@Getter
@Builder
@AllArgsConstructor
public class RegistroAuditoriaResponse {

    private String id;
    private String eventId;
    private String correlationId;
    private TipoEvento tipoEvento;
    private String servicoOrigem;
    private Instant timestamp;
    private Instant dataInsercao;

    public static RegistroAuditoriaResponse fromDomain(RegistroAuditoria r) {
        return RegistroAuditoriaResponse.builder()
                .id(r.getId())
                .eventId(r.getEventId())
                .correlationId(r.getCorrelationId())
                .tipoEvento(r.getTipoEvento())
                .servicoOrigem(r.getServicoOrigem())
                .timestamp(r.getTimestamp())
                .dataInsercao(r.getDataInsercao())
                .build();
    }
}