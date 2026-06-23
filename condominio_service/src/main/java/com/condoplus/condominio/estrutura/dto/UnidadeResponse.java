package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.TipoUnidade;
import com.condoplus.condominio.estrutura.domain.Unidade;

import java.time.LocalDateTime;
import java.util.UUID;

public record UnidadeResponse(
    UUID id,
    String numero,
    String bloco,
    TipoUnidade tipo,
    LocalDateTime criadaEm,
    int totalVinculacoesAtivas
) {
    public static UnidadeResponse fromEntity(Unidade u) {
        int ativas = (int) u.getVinculacoes().stream()
            .filter(v -> v.isAtiva())
            .count();
        return new UnidadeResponse(
            u.getId(), u.getNumero(), u.getBloco(), u.getTipo(), u.getCriadaEm(), ativas
        );
    }
}
