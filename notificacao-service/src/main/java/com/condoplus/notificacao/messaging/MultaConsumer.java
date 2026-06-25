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
public class MultaConsumer {

    private static final Logger log = LoggerFactory.getLogger(MultaConsumer.class);
    private final NotificacaoService notificacaoService;
    private final ObjectMapper objectMapper;

    public MultaConsumer(NotificacaoService notificacaoService, ObjectMapper objectMapper) {
        this.notificacaoService = notificacaoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "multas.aplicadas", groupId = "notificacao-service")
    public void consumir(String mensagemPayload, Acknowledgment ack) {
        log.info("MultaAplicada recebido via Kafka: payload={}", mensagemPayload);

        UUID unidadeId = extrairUnidadeId(mensagemPayload);

        EventoNotificacao en = new EventoNotificacao(
                "evt-multa-" + System.currentTimeMillis(),
                TipoEvento.MULTA_APLICADA,
                "Nova Multa Aplicada",
                "Sua unidade recebeu uma multa. Verifique o aplicativo.",
                unidadeId,
                null,
                Map.of()
        );

        notificacaoService.processarEvento(en)
                .doOnComplete(() -> ack.acknowledge())
                .doOnError(ex -> log.error("Erro processando MultaAplicada.", ex))
                .subscribe();
    }

    private UUID extrairUnidadeId(String raw) {
        try {
            Map<?, ?> map = objectMapper.readValue(raw, Map.class);
            // O CondominioEventProducer publica MultaAplicadaEvent diretamente (JSON flat),
            // com unidadeId na raiz: { "id":"...", "unidadeId":"...", ... }
            Object val = map.get("unidadeId");
            if (val != null) {
                return UUID.fromString(val.toString());
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel extrair unidadeId do payload de multa: {}", e.getMessage());
        }
        return null;
    }
}
