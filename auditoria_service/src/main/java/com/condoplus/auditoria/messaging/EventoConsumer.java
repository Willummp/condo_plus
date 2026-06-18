package com.condoplus.auditoria.messaging;

import com.condoplus.auditoria.anomalia.DetectorAnomalias;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventoConsumer.class);

    private final EventoMapper mapper;
    private final AuditoriaService auditoriaService;
    private final DetectorAnomalias detector;

    public EventoConsumer(EventoMapper mapper, AuditoriaService auditoriaService,
                          DetectorAnomalias detector) {
        this.mapper = mapper;
        this.auditoriaService = auditoriaService;
        this.detector = detector;
    }

    @KafkaListener(topics = "multas.aplicadas", groupId = "auditoria-group")
    public void consumirMultaAplicada(EventEnvelope envelope) { processar(envelope); }

    @KafkaListener(topics = "comunicados.publicados", groupId = "auditoria-group")
    public void consumirComunicadoPublicado(EventEnvelope envelope) { processar(envelope); }

    @KafkaListener(topics = "reservas.confirmadas", groupId = "auditoria-group")
    public void consumirReservaConfirmada(EventEnvelope envelope) { processar(envelope); }

    @KafkaListener(topics = "credenciais.criadas", groupId = "auditoria-group")
    public void consumirCredencialCriada(EventEnvelope envelope) { processar(envelope); }

    @KafkaListener(topics = "logins.falhados", groupId = "auditoria-group")
    public void consumirLoginFalhado(EventEnvelope envelope) { processar(envelope); }

    @KafkaListener(topics = "acessos.registrados", groupId = "auditoria-group")
    public void consumirAcessoRegistrado(EventEnvelope envelope) { processar(envelope); }

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

            RegistroAuditoria salvo = auditoriaService.salvar(mapper.paraRegistro(envelope));
            // Apos arquivar, roda as regras de deteccao sobre o evento.
            detector.avaliar(salvo);
        } finally {
            MDC.remove("correlationId");
        }
    }
}