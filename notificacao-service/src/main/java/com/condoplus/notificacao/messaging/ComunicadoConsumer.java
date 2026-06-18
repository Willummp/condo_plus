package com.condoplus.notificacao.messaging;

import com.condoplus.notificacao.domain.TipoEvento;
import com.condoplus.notificacao.service.EventoNotificacao;
import com.condoplus.notificacao.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComunicadoConsumer {

    private final NotificacaoService notificacaoService;

    @KafkaListener(
            topics = "comunicados.publicados",
            groupId = "notificacao-service"
    )
    public void consumir(String mensagemPayload, Acknowledgment ack) {
        log.info("ComunicadoPublicado recebido via Kafka: payload={}", mensagemPayload);

        String eventId = "evt-" + System.currentTimeMillis();
        EventoNotificacao en = new EventoNotificacao(
                eventId,
                TipoEvento.COMUNICADO_PUBLICADO,
                "Comunicado Geral",
                "Novo comunicado recebido no barramento: " + mensagemPayload,
                null,
                null,
                Map.of("publicoAlvo", "TODOS")
        );
        notificacaoService.processarEvento(en)
                .doOnComplete(() -> {
                    log.debug("Eventos de notificacao enviados para fan-out com sucesso.");
                    ack.acknowledge();
                })
                .doOnError(ex -> {
                    log.error("Erro processando ComunicadoPublicado. NAO confirmando para o Kafka redeliverar.", ex);
                })
                .subscribe();
    }
}