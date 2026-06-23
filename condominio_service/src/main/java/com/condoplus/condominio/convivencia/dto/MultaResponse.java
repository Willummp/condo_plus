package com.condoplus.condominio.convivencia.dto;

import com.condoplus.condominio.convivencia.domain.CategoriaMulta;
import com.condoplus.condominio.convivencia.domain.Multa;
import com.condoplus.condominio.convivencia.domain.StatusMulta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MultaResponse(
    UUID id,
    UUID unidadeId,
    BigDecimal valor,
    String motivo,
    CategoriaMulta categoria,
    String anexoEvidenciaUrl,
    LocalDateTime dataAplicacao,
    LocalDate dataVencimento,
    StatusMulta status,
    UUID aplicadaPorId
) {
    public static MultaResponse fromEntity(Multa m) {
        return new MultaResponse(
            m.getId(),
            m.getUnidadeId() != null ? m.getUnidadeId().getId() : null,
            m.getValor(),
            m.getMotivo(),
            m.getCategoria(),
            m.getAnexoEvidenciaUrl(),
            m.getDataAplicacao(),
            m.getDataVencimento(),
            m.getStatus(),
            m.getAplicadaPorId() != null ? m.getAplicadaPorId().getId() : null
        );
    }
}
