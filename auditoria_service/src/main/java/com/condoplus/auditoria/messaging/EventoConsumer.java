package com.condoplus.auditoria.messaging;

import com.condoplus.auditoria.anomalia.DetectorAnomalias;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.service.AuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public EventoConsumer(EventoMapper mapper, AuditoriaService auditoriaService,
                          DetectorAnomalias detector, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.auditoriaService = auditoriaService;
        this.detector = detector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "multas.aplicadas", groupId = "auditoria-group")
    public void consumirMultaAplicada(String raw) { processar(raw); }

    @KafkaListener(topics = "comunicados.publicados", groupId = "auditoria-group")
    public void consumirComunicadoPublicado(String raw) { processar(raw); }

    @KafkaListener(topics = "reservas.confirmadas", groupId = "auditoria-group")
    public void consumirReservaConfirmada(String raw) { processar(raw); }

    @KafkaListener(topics = "credenciais.criadas", groupId = "auditoria-group")
    public void consumirCredencialCriada(String raw) { processar(raw); }

    @KafkaListener(topics = "logins.falhados", groupId = "auditoria-group")
    public void consumirLoginFalhado(String raw) { processar(raw); }

    @KafkaListener(topics = "acessos.registrados", groupId = "auditoria-group")
    public void consumirAcessoRegistrado(String raw) { processar(raw); }

    @KafkaListener(topics = "visitantes.autorizados", groupId = "auditoria-group")
    public void consumirVisitanteAutorizado(String raw) { processar(raw); }

    @KafkaListener(topics = "logins.realizados", groupId = "auditoria-group")
    public void consumirLoginRealizado(String raw) { processar(raw); }

    private void processar(String raw) {
        if (raw == null || raw.isBlank()) {
            log.warn("Mensagem vazia recebida do Kafka. Ignorando.");
            return;
        }
        try {
            EventEnvelope envelope = objectMapper.readValue(raw, EventEnvelope.class);
            if (envelope.correlationId() != null) {
                MDC.put("correlationId", envelope.correlationId());
            }
            log.info("Evento recebido do Kafka: tipo={}, origem={}, eventId={}",
                    envelope.eventType(), envelope.originService(), envelope.eventId());

            RegistroAuditoria salvo = auditoriaService.salvar(mapper.paraRegistro(envelope));
            detector.avaliar(salvo);
        } catch (Exception e) {
            log.warn("Falha ao processar mensagem Kafka: {}. Raw: {}", e.getMessage(), raw);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
