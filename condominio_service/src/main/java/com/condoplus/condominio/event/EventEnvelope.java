package com.condoplus.condominio.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        LocalDateTime timestamp,
        String correlationId,
        String originService,
        T payload
) {
    public static <T> EventEnvelope<T> wrap(String eventType, String correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                LocalDateTime.now(),
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                "condominio-service",
                payload
        );
    }
}
