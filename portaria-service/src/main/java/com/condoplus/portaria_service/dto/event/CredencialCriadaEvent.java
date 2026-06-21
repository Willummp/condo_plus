package com.condoplus.portaria_service.dto.event;

import java.time.Instant;
import java.util.List;

public record CredencialCriadaEvent(
        String eventId,
        Instant timestamp,
        String correlationId,
        String credencialId,
        List<String> roles
) {}