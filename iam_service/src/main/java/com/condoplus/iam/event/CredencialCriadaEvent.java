package com.condoplus.iam.event;

import java.util.Set;
import java.util.UUID;

public record CredencialCriadaEvent(
        UUID credencialId,
        String email,
        Set<String> roles,
        String correlationId
) {}
