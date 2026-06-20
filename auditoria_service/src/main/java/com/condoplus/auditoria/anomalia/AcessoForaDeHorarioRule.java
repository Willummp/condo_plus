package com.condoplus.auditoria.anomalia;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.SeveridadeAnomalia;
import com.condoplus.auditoria.domain.StatusAnomalia;
import com.condoplus.auditoria.domain.TipoEvento;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Regra: acesso registrado na madrugada (00h-05h) e atipico -> WARNING.
 *
 * SEM ESTADO (stateless): nao consulta historico nem janela. O proprio evento
 * ja contem tudo — a hora do acesso. Contraste com LoginFalhadoRule (que precisa
 * de janela). Mesmo framework acomoda os dois tipos de regra.
 *
 * A hora e avaliada no FUSO DO CONDOMINIO (America/Sao_Paulo), nao em UTC:
 * o timestamp chega em UTC e "madrugada" so faz sentido no horario local.
 */
@Component
public class AcessoForaDeHorarioRule implements RegraAnomalia {

    private static final ZoneId FUSO_CONDOMINIO = ZoneId.of("America/Sao_Paulo");
    private static final int INICIO_MADRUGADA = 0;  // 00h inclusive
    private static final int FIM_MADRUGADA = 5;     // ate 04h59 (5h exclusive)

    @Override
    public Optional<Anomalia> avaliar(RegistroAuditoria evento) {
        if (evento.getTipoEvento() != TipoEvento.ACESSO_REGISTRADO
                || evento.getTimestamp() == null) {
            return Optional.empty();
        }

        Instant ts = evento.getTimestamp();
        LocalTime horaLocal = ts.atZone(FUSO_CONDOMINIO).toLocalTime();
        int hora = horaLocal.getHour();

        if (hora >= INICIO_MADRUGADA && hora < FIM_MADRUGADA) {
            String quem = (evento.getEntidadeAfetada() != null
                    && evento.getEntidadeAfetada().getId() != null)
                    ? evento.getEntidadeAfetada().getId()
                    : "entidade desconhecida";

            Anomalia anomalia = Anomalia.builder()
                    .tipoRegra("ACESSO_FORA_DE_HORARIO")
                    .severidade(SeveridadeAnomalia.WARNING)
                    .descricao("Acesso registrado as " + horaLocal
                            + " (madrugada, fuso do condominio) - entidade: " + quem)
                    .entidadeRelacionada(quem)
                    .correlationId(evento.getCorrelationId())
                    .eventoGatilhoId(evento.getEventId())
                    .status(StatusAnomalia.ABERTA)
                    .build();
            return Optional.of(anomalia);
        }
        return Optional.empty();
    }
}