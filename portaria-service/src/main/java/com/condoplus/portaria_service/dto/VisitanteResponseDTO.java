package com.condoplus.portaria_service.dto;
import com.condoplus.portaria_service.model.enums.StatusVisitante;
import com.condoplus.portaria_service.model.enums.TipoVisitante;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitanteResponseDTO(
        UUID id,
        String nome,
        String documento,
        TipoVisitante tipo,
        UUID autorizadoParaUnidadeId,
        LocalDateTime validadeInicio,
        LocalDateTime validadeFim,
        StatusVisitante status
) {}
