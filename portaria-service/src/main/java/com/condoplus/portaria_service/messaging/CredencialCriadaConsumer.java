package com.condoplus.portaria_service.messaging;

import com.condoplus.portaria_service.dto.event.CredencialCriadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CredencialCriadaConsumer {

    @KafkaListener(
            topics = "${condoplus.kafka.topics.credenciais-criadas}",
            groupId = "portaria-service"
    )
    public void consumir(CredencialCriadaEvent evento) {
        log.info("CredencialCriada recebida. credencialId={} roles={}",
                evento.credencialId(), evento.roles());
    }
}