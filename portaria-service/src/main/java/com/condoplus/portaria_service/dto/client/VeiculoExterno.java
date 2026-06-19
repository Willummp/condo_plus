package com.condoplus.portaria_service.dto.client;

import java.util.UUID;

public record VeiculoExterno(
        UUID id,
        String placa,
        String modelo,
        String cor,
        UUID unidadeId,
        boolean ativo
) {}
