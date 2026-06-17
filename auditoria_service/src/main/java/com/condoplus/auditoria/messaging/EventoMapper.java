package com.condoplus.auditoria.messaging;

import com.condoplus.auditoria.domain.EntidadeAfetada;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.TipoEvento;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Converte um EventEnvelope (vindo do Kafka) num RegistroAuditoria.
 *
 * Centraliza a traducao num so lugar para o consumer (C16) ficar enxuto.
 * O mapeamento e generico: serve para qualquer tipo de evento, porque o
 * auditoria nao precisa entender o negocio de cada um — so arquivar.
 */
@Component
public class EventoMapper {

    public RegistroAuditoria paraRegistro(EventEnvelope envelope) {
        return RegistroAuditoria.builder()
                .eventId(envelope.eventId())
                .correlationId(envelope.correlationId())
                .timestamp(envelope.timestamp() != null ? envelope.timestamp() : Instant.now())
                .tipoEvento(mapearTipo(envelope.eventType()))
                .servicoOrigem(envelope.originService())
                .entidadeAfetada(extrairEntidade(envelope))
                .payload(envelope.payload())
                .build();
    }

    /**
     * Traduz a String eventType do envelope para o enum TipoEvento.
     * Se chegar um tipo que ainda nao mapeamos, cai em OUTRO (o fallback
     * do C4) — o evento e arquivado mesmo assim, sem perder informacao.
     */
    private TipoEvento mapearTipo(String eventType) {
        if (eventType == null) {
            return TipoEvento.OUTRO;
        }
        return switch (eventType) {
            case "MultaAplicada" -> TipoEvento.MULTA_APLICADA;
            case "ComunicadoPublicado" -> TipoEvento.COMUNICADO_PUBLICADO;
            case "ReservaConfirmada" -> TipoEvento.RESERVA_CONFIRMADA;
            case "CredencialCriada" -> TipoEvento.CREDENCIAL_CRIADA;
            case "LoginFalhado" -> TipoEvento.LOGIN_FALHADO;   // <-- adicionar esta linha
            default -> TipoEvento.OUTRO;
        };
    }

    /**
     * Tenta identificar a entidade afetada a partir do payload, de forma
     * best-effort. Como cada evento tem campos diferentes, procuramos as
     * chaves mais comuns. Se nao achar, retorna null (campo opcional).
     */
    private EntidadeAfetada extrairEntidade(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        if (payload == null) {
            return null;
        }
        // Heuristica simples: o "id" do payload identifica a entidade,
        // e o eventType sugere o tipo dela.
        Object id = payload.get("id");
        if (id == null) {
            return null;
        }
        return EntidadeAfetada.builder()
                .tipo(envelope.eventType()) // ex: "MultaAplicada"
                .id(id.toString())
                .build();
    }
}