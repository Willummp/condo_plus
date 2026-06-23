package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.TipoUnidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NovaUnidadeRequest(
    @NotBlank @Size(max = 20) String numero,
    @Size(max = 20) String bloco,
    @NotNull TipoUnidade tipo
) {}
