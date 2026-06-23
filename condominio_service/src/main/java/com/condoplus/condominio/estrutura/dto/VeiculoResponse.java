package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.Veiculo;

import java.util.UUID;

public record VeiculoResponse(
    UUID id,
    String placa,
    String modelo,
    String cor,
    UUID unidadeId,
    boolean ativo
) {
    public static VeiculoResponse fromEntity(Veiculo v) {
        return new VeiculoResponse(
            v.getId(),
            v.getPlaca(),
            v.getModelo(),
            v.getCor(),
            v.getUnidadeId() != null ? v.getUnidadeId().getId() : null,
            v.isAtivo()
        );
    }
}
