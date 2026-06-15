package com.condoplus.portaria_service.mapper;

import com.condoplus.portaria_service.dto.CriarEncomendaDTO;
import com.condoplus.portaria_service.dto.EncomendaResponseDTO;
import com.condoplus.portaria_service.model.entities.Encomenda;

public class EncomendaMapper {

    public static Encomenda toEntity(CriarEncomendaDTO dto) {
        if (dto == null) return null;

        return Encomenda.builder()
                .unidadeId(dto.unidadeId())
                .tipo(dto.tipo())
                .descricao(dto.descricao())
                .codigoRastreio(dto.codigoRastreio())
                .porteiroRecebedorId(dto.porteiroRecebedorId())
                .build();
    }

    public static EncomendaResponseDTO toResponse(Encomenda entity) {
        if (entity == null) return null;

        return new EncomendaResponseDTO(
                entity.getId(),
                entity.getUnidadeId(),
                entity.getTipo(),
                entity.getDescricao(),
                entity.getStatus(),
                entity.getDataChegada(),
                entity.getDataRetirada()
        );
    }
}