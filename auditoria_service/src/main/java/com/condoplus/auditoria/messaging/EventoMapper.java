package com.condoplus.auditoria.messaging;

import com.condoplus.auditoria.domain.EntidadeAfetada;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.TipoEvento;
import com.condoplus.auditoria.domain.PessoaIniciadora;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;


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
                .timestamp(parseTimestamp(envelope.timestamp()))
                .tipoEvento(mapearTipo(envelope.eventType()))
                .servicoOrigem(envelope.originService())
                .entidadeAfetada(extrairEntidade(envelope))
                .pessoaIniciadora(extrairIniciador(envelope))
                .payload(envelope.payload())
                .build();
    }

    private Instant parseTimestamp(Object ts) {
        if (ts == null) return Instant.now();
        String s = ts.toString();
        try {
            return Instant.parse(s);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC);
            } catch (Exception e2) {
                return Instant.now();
            }
        }
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
            case "LoginFalhado" -> TipoEvento.LOGIN_FALHADO;
            case "AcessoRegistrado" -> TipoEvento.ACESSO_REGISTRADO;
            case "VisitanteAutorizado" -> TipoEvento.VISITANTE_AUTORIZADO;
            case "LoginRealizado" -> TipoEvento.LOGIN_REALIZADO;
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
    /**
     * Extrai o iniciador (quem agiu) do payload, se presente.
     * Espera os campos iniciadorId (UUID), iniciadorNome e iniciadorRoles.
     * Tolerante: payload sem iniciador, ou com UUID invalido, retorna null —
     * o evento e arquivado mesmo assim (auditoria nunca descarta o evento).
     */
    private PessoaIniciadora extrairIniciador(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        if (payload == null || payload.get("iniciadorId") == null) {
            return null;
        }
        try {
            UUID id = UUID.fromString(payload.get("iniciadorId").toString());
            Object nome = payload.get("iniciadorNome");
            Object roles = payload.get("iniciadorRoles");
            return PessoaIniciadora.builder()
                    .id(id)
                    .nomeCached(nome != null ? nome.toString() : null)
                    .roles(roles != null ? roles.toString() : null)
                    .build();
        } catch (IllegalArgumentException e) {
            return null; // iniciadorId nao e UUID valido: tolera e segue
        }
    }
}