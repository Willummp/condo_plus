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
public class EncomendaConsumer {

    private static final Logger log = LoggerFactory.getLogger(EncomendaConsumer.class);
    private final NotificacaoService notificacaoService;

    public EncomendaConsumer(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @KafkaListener(topics = "encomendas.recebidas", groupId = "notificacao-service")
    public void consumir(String mensagemPayload, Acknowledgment ack) {
        log.info("EncomendaRecebida recebido via Kafka: payload={}", mensagemPayload);

        EventoNotificacao en = new EventoNotificacao(
                "evt-encomenda-" + System.currentTimeMillis(),
                TipoEvento.ENCOMENDA_RECEBIDA,
                "Encomenda Recebida",
                "Uma nova encomenda chegou: " + mensagemPayload,
                null,
                null,
                Map.of()
        );

        notificacaoService.processarEvento(en)
                .doOnComplete(() -> ack.acknowledge())
                .doOnError(ex -> log.error("Erro processando EncomendaRecebida.", ex))
                .subscribe();
    }
}
