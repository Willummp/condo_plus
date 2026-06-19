package com.condoplus.portaria_service.dto.response;

import com.condoplus.portaria_service.model.entities.Visitante;
import com.condoplus.portaria_service.model.enums.StatusVisitante;
import com.condoplus.portaria_service.model.enums.TipoVisitante;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitanteResponseDTO(
        UUID id,
        String nome,
        String documento,
        TipoVisitante tipo,
        UUID unidadeId,
        LocalDateTime validadeInicio,
        LocalDateTime validadeFim,
        StatusVisitante status,
        LocalDateTime criadoEm
) {
    public static VisitanteResponseDTO fromEntity(Visitante v) {
        return new VisitanteResponseDTO(
                v.getId(),
                v.getNome(),
                v.getDocumento(),
                v.getTipo(),
                v.getAutorizadoParaUnidadeId(),
                v.getValidadeInicio(),
                v.getValidadeFim(),
                v.getStatus(),
                v.getCriadoEm()
        );
    }
}
