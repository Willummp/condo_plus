package com.condoplus.portaria_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EntradaVisitanteRequest(
        @NotNull UUID visitanteId,
        @Size(max = 500) String observacoes
) {}
