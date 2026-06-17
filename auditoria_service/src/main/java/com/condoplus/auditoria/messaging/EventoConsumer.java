package com.condoplus.auditoria.messaging;

import com.condoplus.auditoria.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome eventos de dominio dos demais servicos e os arquiva.
 *
 * Um @KafkaListener por topico real publicado pelo grupo. Cada um recebe o
 * EventEnvelope (ja desserializado pelo nosso ConsumerFactory), restaura o
 * correlationId no MDC (rastreamento nos logs), delega ao EventoMapper a
 * conversao para RegistroAuditoria e ao AuditoriaService a persistencia
 * idempotente.
 *
 * O auditoria e o servico com maior consumo Kafka do projeto: e o ponto
 * natural de correlacao, pois enxerga eventos de todos os servicos.
 */
@Component
public class EventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventoConsumer.class);

    private final EventoMapper mapper;
    private final AuditoriaService auditoriaService;

    public EventoConsumer(EventoMapper mapper, AuditoriaService auditoriaService) {
        this.mapper = mapper;
        this.auditoriaService = auditoriaService;
    }

    @KafkaListener(topics = "multas.aplicadas", groupId = "auditoria-group")
    public void consumirMultaAplicada(EventEnvelope envelope) {
        processar(envelope);
    }

    @KafkaListener(topics = "comunicados.publicados", groupId = "auditoria-group")
    public void consumirComunicadoPublicado(EventEnvelope envelope) {
        processar(envelope);
    }

    @KafkaListener(topics = "reservas.confirmadas", groupId = "auditoria-group")
    public void consumirReservaConfirmada(EventEnvelope envelope) {
        processar(envelope);
    }

    @KafkaListener(topics = "credenciais.criadas", groupId = "auditoria-group")
    public void consumirCredencialCriada(EventEnvelope envelope) {
        processar(envelope);
    }

    /**
     * Fluxo comum a todos os topicos: restaura correlationId, mapeia e salva.
     * Um unico metodo porque o auditoria trata todo evento da mesma forma
     * (metadados + payload) — coerente com a Opcao B.
     */
    private void processar(EventEnvelope envelope) {
        if (envelope == null) {
            log.warn("Envelope nulo recebido (provavel mensagem malformada). Ignorando.");
            return;
        }
        try {
            if (envelope.correlationId() != null) {
                MDC.put("correlationId", envelope.correlationId());
            }
            log.info("Evento recebido do Kafka: tipo={}, origem={}, eventId={}",
                    envelope.eventType(), envelope.originService(), envelope.eventId());

            auditoriaService.salvar(mapper.paraRegistro(envelope));
        } finally {
            MDC.remove("correlationId");
        }
    }
}