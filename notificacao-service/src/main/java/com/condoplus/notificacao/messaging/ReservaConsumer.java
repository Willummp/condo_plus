package com.condoplus.notificacao.messaging;

import com.condoplus.notificacao.domain.TipoEvento;
import com.condoplus.notificacao.service.EventoNotificacao;
import com.condoplus.notificacao.service.NotificacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class ReservaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservaConsumer.class);
    private final NotificacaoService notificacaoService;
    private final ObjectMapper objectMapper;

    public ReservaConsumer(NotificacaoService notificacaoService, ObjectMapper objectMapper) {
        this.notificacaoService = notificacaoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "reservas.confirmadas", groupId = "notificacao-service")
    public void consumir(String mensagemPayload, Acknowledgment ack) {
        log.info("ReservaConfirmada recebido via Kafka: payload={}", mensagemPayload);

        UUID moradorId = extrairMoradorId(mensagemPayload);

        EventoNotificacao en = new EventoNotificacao(
                "evt-reserva-" + System.currentTimeMillis(),
                TipoEvento.RESERVA_CONFIRMADA,
                "Reserva Confirmada",
                "Sua reserva de área comum foi confirmada.",
                null,
                moradorId,
                Map.of()
        );

        notificacaoService.processarEvento(en)
                .doOnComplete(() -> ack.acknowledge())
                .doOnError(ex -> log.error("Erro processando ReservaConfirmada.", ex))
                .subscribe();
    }

    private UUID extrairMoradorId(String raw) {
        try {
            Map<?, ?> envelope = objectMapper.readValue(raw, Map.class);
            Map<?, ?> payload = (Map<?, ?>) envelope.get("payload");
            if (payload != null && payload.get("moradorId") != null) {
                return UUID.fromString(payload.get("moradorId").toString());
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel extrair moradorId do payload de reserva: {}", e.getMessage());
        }
        return null;
    }
}
