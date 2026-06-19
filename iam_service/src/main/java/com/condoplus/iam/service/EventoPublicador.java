package com.condoplus.iam.service;

import com.condoplus.iam.event.CredencialCriadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventoPublicador {

    private static final String TOPICO_CREDENCIAIS_CRIADAS = "credenciais.criadas";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publicarCredencialCriada(CredencialCriadaEvent evento) {
        kafkaTemplate.send(TOPICO_CREDENCIAIS_CRIADAS, evento.credencialId().toString(), evento)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar CredencialCriada. credencialId={}", evento.credencialId(), ex);
                    } else {
                        log.debug("CredencialCriada publicado. credencialId={}", evento.credencialId());
                    }
                });
    }
}
