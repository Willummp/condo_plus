package com.condoplus.portaria_service.dto;
import com.condoplus.portaria_service.model.enums.StatusEncomenda;
import com.condoplus.portaria_service.model.enums.TipoEncomenda;

import java.time.LocalDateTime;
import java.util.UUID;

public record EncomendaResponseDTO(
        UUID id,
        UUID unidadeId,
        TipoEncomenda tipo,
        String descricao,
        StatusEncomenda status,
        LocalDateTime dataChegada,
        LocalDateTime dataRetirada
) {}