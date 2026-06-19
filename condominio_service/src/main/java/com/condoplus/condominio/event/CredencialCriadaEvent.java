package com.condoplus.condominio.event;

import java.util.UUID;

public record CredencialCriadaEvent(
        UUID credencialId,
        String email,
        String documento,
        String nomeCompleto,
        String telefone,
        String role,
        String correlationId
) {}
