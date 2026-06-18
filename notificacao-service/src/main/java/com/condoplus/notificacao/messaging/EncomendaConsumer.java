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
public class EncomendaConsumer {

    private final NotificacaoService notificacaoService;

    @KafkaListener(
            topics = "encomendas.recebidas",
            groupId = "notificacao-service"
    )
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
