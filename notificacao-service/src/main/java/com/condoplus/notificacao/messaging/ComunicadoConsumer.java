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
public class ComunicadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(ComunicadoConsumer.class);
    private final NotificacaoService notificacaoService;

    public ComunicadoConsumer(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @KafkaListener(topics = "comunicados.publicados", groupId = "notificacao-service")
    public void consumir(String mensagemPayload, Acknowledgment ack) {
        log.info("ComunicadoPublicado recebido via Kafka: payload={}", mensagemPayload);

        String eventId = "evt-" + System.currentTimeMillis();
        EventoNotificacao en = new EventoNotificacao(
                eventId, TipoEvento.COMUNICADO_PUBLICADO, "Comunicado Geral",
                "Novo comunicado recebido no barramento: " + mensagemPayload,
                null, null, Map.of("publicoAlvo", "TODOS")
        );

        notificacaoService.processarEvento(en)
                .doOnComplete(() -> ack.acknowledge())
                .doOnError(ex -> log.error("Erro processando ComunicadoPublicado.", ex))
                .subscribe();
    }
}