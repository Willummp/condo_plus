package com.condoplus.portaria_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RetiradaRequest(@NotNull UUID retiradoPorPessoaId) {}
