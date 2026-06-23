package com.condoplus.condominio.convivencia.dto;

import com.condoplus.condominio.convivencia.domain.AreaComum;

import java.math.BigDecimal;
import java.util.UUID;

public record AreaComumResponse(
    UUID id,
    String nome,
    Integer capacidadeMaxima,
    BigDecimal valorReserva,
    String regras,
    boolean ativa
) {
    public static AreaComumResponse fromEntity(AreaComum a) {
        return new AreaComumResponse(
            a.getId(),
            a.getNome(),
            a.getCapacidadeMaxima(),
            a.getValorReserva(),
            a.getRegras(),
            a.isAtiva()
        );
    }
}
