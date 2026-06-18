package com.condoplus.notificacao.messaging;

import com.condoplus.notificacao.domain.TipoEvento;
import com.condoplus.notificacao.service.EventoNotificacao;
import com.condoplus.notificacao.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class MultaConsumer {

    private static final Logger log = LoggerFactory.getLogger(MultaConsumer.class);
    private final NotificacaoService notificacaoService;

    public MultaConsumer(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @KafkaListener(topics = "multas.aplicadas", groupId = "notificacao-service")
    public void consumir(String mensagemPayload, Acknowledgment ack) {
        log.info("MultaAplicada recebido via Kafka: payload={}", mensagemPayload);

        EventoNotificacao en = new EventoNotificacao(
                "evt-multa-" + System.currentTimeMillis(),
                TipoEvento.MULTA_APLICADA,
                "Nova Multa Aplicada",
                "Detalhes da multa: " + mensagemPayload,
                null,
                null,
                Map.of()
        );

        notificacaoService.processarEvento(en)
                .doOnComplete(() -> ack.acknowledge())
                .doOnError(ex -> log.error("Erro processando MultaAplicada.", ex))
                .subscribe();
    }
}
