package com.condoplus.auditoria.anomalia;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.SeveridadeAnomalia;
import com.condoplus.auditoria.domain.StatusAnomalia;
import com.condoplus.auditoria.domain.TipoEvento;
import com.condoplus.auditoria.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Regra: muitos logins falhados da mesma credencial em janela curta = forca bruta.
 *
 * Janela DESLIZANTE de 60s, relativa ao TIMESTAMP DO EVENTO (nao ao relogio
 * do servidor): robusto a atraso de propagacao e deterministico para teste.
 *
 * Dispara apenas quando o contador CRUZA o limiar (== 5 -> WARNING,
 * == 10 -> CRITICAL), para nao gerar uma anomalia a cada novo evento depois disso.
 */
@Component
public class LoginFalhadoRule implements RegraAnomalia {

    private static final long JANELA_SEGUNDOS = 60;
    private static final long LIMIAR_WARNING = 5;
    private static final long LIMIAR_CRITICAL = 10;

    private final RegistroAuditoriaRepository repository;

    public LoginFalhadoRule(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Anomalia> avaliar(RegistroAuditoria evento) {
        if (evento.getTipoEvento() != TipoEvento.LOGIN_FALHADO
                || evento.getEntidadeAfetada() == null
                || evento.getEntidadeAfetada().getId() == null) {
            return Optional.empty();
        }

        String credencial = evento.getEntidadeAfetada().getId();
        Instant base = evento.getTimestamp() != null ? evento.getTimestamp() : Instant.now();
        Instant inicioJanela = base.minusSeconds(JANELA_SEGUNDOS);

        long falhas = repository.countByTipoEventoAndEntidadeAfetada_IdAndTimestampGreaterThanEqual(
                TipoEvento.LOGIN_FALHADO, credencial, inicioJanela);

        if (falhas == LIMIAR_CRITICAL) {
            return Optional.of(criar(evento, credencial, falhas, SeveridadeAnomalia.CRITICAL));
        }
        if (falhas == LIMIAR_WARNING) {
            return Optional.of(criar(evento, credencial, falhas, SeveridadeAnomalia.WARNING));
        }
        return Optional.empty();
    }

    private Anomalia criar(RegistroAuditoria evento, String credencial, long falhas, SeveridadeAnomalia sev) {
        return Anomalia.builder()
                .tipoRegra("LOGIN_FALHADO_EXCESSIVO")
                .severidade(sev)
                .descricao(falhas + " logins falhados da credencial '" + credencial
                        + "' em ate " + JANELA_SEGUNDOS + "s")
                .entidadeRelacionada(credencial)
                .correlationId(evento.getCorrelationId())
                .eventoGatilhoId(evento.getEventId())
                .status(StatusAnomalia.ABERTA)
                .build();
    }
}