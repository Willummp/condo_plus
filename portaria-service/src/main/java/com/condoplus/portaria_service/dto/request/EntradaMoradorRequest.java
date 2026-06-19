package com.condoplus.portaria_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EntradaMoradorRequest(
        @NotNull UUID moradorId,
        @NotNull UUID unidadeId,
        @Pattern(regexp = "[A-Z0-9]{7}", message = "Placa deve ter 7 caracteres alfanuméricos")
        String veiculoPlaca,
        @Size(max = 500) String observacoes
) {}
