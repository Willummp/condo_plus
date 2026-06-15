package com.condoplus.portaria_service.mapper;

import com.condoplus.portaria_service.dto.CriarVisitanteDTO;
import com.condoplus.portaria_service.dto.VisitanteResponseDTO;
import com.condoplus.portaria_service.model.entities.Visitante;

public class VisitanteMapper {

    public static Visitante toEntity(CriarVisitanteDTO dto) {
        if (dto == null) return null;

        return Visitante.builder()
                .nome(dto.nome())
                .documento(dto.documento())
                .telefone(dto.telefone())
                .tipo(dto.tipo())
                .autorizadoPorPessoaId(dto.autorizadoPorPessoaId())
                .autorizadoParaUnidadeId(dto.autorizadoParaUnidadeId())
                .validadeInicio(dto.validadeInicio())
                .validadeFim(dto.validadeFim())
                .build();
    }

    public static VisitanteResponseDTO toResponse(Visitante entity) {
        if (entity == null) return null;

        return new VisitanteResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDocumento(),
                entity.getTipo(),
                entity.getAutorizadoParaUnidadeId(),
                entity.getValidadeInicio(),
                entity.getValidadeFim(),
                entity.getStatus()
        );
    }
}