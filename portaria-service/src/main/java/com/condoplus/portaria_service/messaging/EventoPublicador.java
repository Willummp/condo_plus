package com.condoplus.portaria_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventoPublicador {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publicar(String topico, String chave, Object evento) {
        kafkaTemplate.send(topico, chave, evento)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar evento. topico={}", topico, ex);
                    } else {
                        log.debug("Evento publicado. topico={} offset={}",
                                topico, result.getRecordMetadata().offset());
                    }
                });
    }
}