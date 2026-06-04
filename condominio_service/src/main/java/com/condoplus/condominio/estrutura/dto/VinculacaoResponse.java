package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.Escopo;
import com.condoplus.condominio.estrutura.domain.StatusVinculacao;
import com.condoplus.condominio.estrutura.domain.TipoVinculacao;
import com.condoplus.condominio.estrutura.domain.Vinculacao;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record VinculacaoResponse(
    UUID id,
    UUID pessoaId,
    TipoVinculacao tipo,
    StatusVinculacao status,
    LocalDate dataInicio,
    LocalDate dataFim,
    Set<Escopo> escopos
) {
    public static VinculacaoResponse fromEntity(Vinculacao v) {
        return new VinculacaoResponse(
            v.getId(),
            v.getPessoaId().getId(),
            v.getTipo(),
            v.getStatus(),
            v.getDataInicio(),
            v.getDataFim(),
            v.getEscoposComoEnum()
        );
    }
}
