package com.condoplus.portaria_service.dto.response;

import com.condoplus.portaria_service.model.entities.RegistroAcesso;
import com.condoplus.portaria_service.model.enums.TipoMovimento;
import com.condoplus.portaria_service.model.enums.TipoPessoaAcesso;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistroAcessoResponseDTO(
        UUID id,
        TipoPessoaAcesso tipoPessoa,
        UUID pessoaId,
        UUID unidadeId,
        String veiculoPlaca,
        TipoMovimento tipoMovimento,
        LocalDateTime timestamp,
        UUID porteiroId
) {
    public static RegistroAcessoResponseDTO fromEntity(RegistroAcesso r) {
        return new RegistroAcessoResponseDTO(
                r.getId(),
                r.getTipoPessoa(),
                r.getPessoaId(),
                r.getUnidadeId(),
                r.getVeiculoPlaca(),
                r.getTipoMovimento(),
                r.getTimestampAcesso(),
                r.getPorteiroId()
        );
    }
}
