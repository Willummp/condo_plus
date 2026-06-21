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
public class EncomendaConsumer {

    private static final Logger log = LoggerFactory.getLogger(EncomendaConsumer.class);
    private final NotificacaoService notificacaoService;
    private final ObjectMapper objectMapper;

    public EncomendaConsumer(NotificacaoService notificacaoService, ObjectMapper objectMapper) {
        this.notificacaoService = notificacaoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "encomendas.recebidas", groupId = "notificacao-service")
    public void consumir(String mensagemPayload, Acknowledgment ack) {
        log.info("EncomendaRecebida recebido via Kafka: payload={}", mensagemPayload);

        UUID unidadeId = extrairUnidadeId(mensagemPayload);

        EventoNotificacao en = new EventoNotificacao(
                "evt-encomenda-" + System.currentTimeMillis(),
                TipoEvento.ENCOMENDA_RECEBIDA,
                "Encomenda Recebida",
                "Chegou uma encomenda para sua unidade na portaria.",
                unidadeId,
                null,
                Map.of()
        );

        notificacaoService.processarEvento(en)
                .doOnComplete(() -> ack.acknowledge())
                .doOnError(ex -> log.error("Erro processando EncomendaRecebida.", ex))
                .subscribe();
    }

    private UUID extrairUnidadeId(String raw) {
        try {
            Map<?, ?> envelope = objectMapper.readValue(raw, Map.class);
            Map<?, ?> payload = (Map<?, ?>) envelope.get("payload");
            if (payload != null && payload.get("unidadeId") != null) {
                return UUID.fromString(payload.get("unidadeId").toString());
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel extrair unidadeId do payload de encomenda: {}", e.getMessage());
        }
        return null;
    }
}
