package com.condoplus.notificacao.messaging;

import com.condoplus.notificacao.domain.TipoEvento;
import com.condoplus.notificacao.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

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
        log.info("Evento Kafka recebido no topico comunicados.publicados: payload={}", mensagemPayload);

        try {
            log.debug("Processando evento de notificacao para fan-out de comunicados...");

            ack.acknowledge();
            log.info("Mensagem confirmada com sucesso no Kafka (Acknowledge enviado).");

        } catch (Exception ex) {
            log.error("Erro processando ComunicadoPublicado. NAO confirmando para o Kafka redeliverar.", ex);
        }
    }
}
