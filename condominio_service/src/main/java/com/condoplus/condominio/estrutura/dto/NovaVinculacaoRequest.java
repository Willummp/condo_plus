package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.TipoVinculacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.UUID;

public record NovaVinculacaoRequest(
    @NotNull UUID pessoaId,
    @NotNull TipoVinculacao tipo,
    @NotNull @PastOrPresent LocalDate dataInicio
) {}
