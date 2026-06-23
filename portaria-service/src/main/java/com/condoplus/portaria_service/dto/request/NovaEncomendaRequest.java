package com.condoplus.portaria_service.dto.request;

import com.condoplus.portaria_service.model.enums.TipoEncomenda;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NovaEncomendaRequest(
        @NotNull UUID unidadeId,
        @NotNull TipoEncomenda tipo,
        @Size(max = 500) String descricao,
        @Size(max = 100) String codigoRastreio
) {}
