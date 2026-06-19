package com.condoplus.portaria_service.dto.response;

import com.condoplus.portaria_service.model.entities.Encomenda;
import com.condoplus.portaria_service.model.enums.StatusEncomenda;
import com.condoplus.portaria_service.model.enums.TipoEncomenda;

import java.time.LocalDateTime;
import java.util.UUID;

public record EncomendaResponseDTO(
        UUID id,
        UUID unidadeId,
        TipoEncomenda tipo,
        String descricao,
        String codigoRastreio,
        StatusEncomenda status,
        LocalDateTime dataChegada,
        LocalDateTime dataRetirada
) {
    public static EncomendaResponseDTO fromEntity(Encomenda e) {
        return new EncomendaResponseDTO(
                e.getId(),
                e.getUnidadeId(),
                e.getTipo(),
                e.getDescricao(),
                e.getCodigoRastreio(),
                e.getStatus(),
                e.getDataChegada(),
                e.getDataRetirada()
        );
    }
}
