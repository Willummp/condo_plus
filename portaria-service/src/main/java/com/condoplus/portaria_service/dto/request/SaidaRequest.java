package com.condoplus.portaria_service.dto.request;

import com.condoplus.portaria_service.model.enums.TipoPessoaAcesso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SaidaRequest(
        @NotNull TipoPessoaAcesso tipoPessoa,
        @NotNull UUID pessoaId,
        UUID unidadeId,
        String veiculoPlaca,
        @Size(max = 500) String observacoes
) {}
