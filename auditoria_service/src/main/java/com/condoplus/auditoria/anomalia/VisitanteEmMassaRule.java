package com.condoplus.auditoria.anomalia;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.PessoaIniciadora;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.SeveridadeAnomalia;
import com.condoplus.auditoria.domain.StatusAnomalia;
import com.condoplus.auditoria.domain.TipoEvento;
import com.condoplus.auditoria.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Regra: um morador que autoriza muitos visitantes em janela curta e suspeito
 * (ex: conta comprometida, venda de acesso).
 *
 * Janela DESLIZANTE de 1 HORA, agrupada por pessoaIniciadora.id — o ATOR.
 * Contraste com LoginFalhadoRule: la a janela e por entidade-alvo (vitima),
 * aqui e por iniciador (quem age). Mesma tecnica, dimensao de agrupamento oposta.
 *
 * Dispara em == 10 (WARNING). Igualdade exata evita uma anomalia por evento
 * apos o limiar (contagem sequencial garantida pela particao unica do topico).
 */
@Component
public class VisitanteEmMassaRule implements RegraAnomalia {

    private static final long JANELA_SEGUNDOS = 3600; // 1 hora
    private static final long LIMIAR = 10;

    private final RegistroAuditoriaRepository repository;

    public VisitanteEmMassaRule(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Anomalia> avaliar(RegistroAuditoria evento) {
        if (evento.getTipoEvento() != TipoEvento.VISITANTE_AUTORIZADO
                || evento.getPessoaIniciadora() == null
                || evento.getPessoaIniciadora().getId() == null) {
            return Optional.empty();
        }

        PessoaIniciadora iniciador = evento.getPessoaIniciadora();
        UUID iniciadorId = iniciador.getId();
        Instant base = evento.getTimestamp() != null ? evento.getTimestamp() : Instant.now();
        Instant inicioJanela = base.minusSeconds(JANELA_SEGUNDOS);

        long autorizacoes = repository.countByTipoEventoAndPessoaIniciadora_IdAndTimestampGreaterThanEqual(
                TipoEvento.VISITANTE_AUTORIZADO, iniciadorId, inicioJanela);

        if (autorizacoes == LIMIAR) {
            String quem = iniciador.getNomeCached() != null
                    ? iniciador.getNomeCached() + " (" + iniciadorId + ")"
                    : iniciadorId.toString();
            Anomalia anomalia = Anomalia.builder()
                    .tipoRegra("AUTORIZACAO_VISITANTES_EM_MASSA")
                    .severidade(SeveridadeAnomalia.WARNING)
                    .descricao(autorizacoes + " visitantes autorizados por " + quem + " em ate 1h")
                    .entidadeRelacionada(iniciadorId.toString())
                    .correlationId(evento.getCorrelationId())
                    .eventoGatilhoId(evento.getEventId())
                    .status(StatusAnomalia.ABERTA)
                    .build();
            return Optional.of(anomalia);
        }
        return Optional.empty();
    }
}