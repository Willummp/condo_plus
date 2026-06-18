package com.condoplus.auditoria.anomalia;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.SeveridadeAnomalia;
import com.condoplus.auditoria.domain.StatusAnomalia;
import com.condoplus.auditoria.domain.TipoEvento;
import com.condoplus.auditoria.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Regra: login bem-sucedido logo apos uma rajada de falhas vindas de OUTRO IP
 * = forte indicio de invasao de conta (account takeover / viagem impossivel).
 * Severidade CRITICAL.
 *
 * Unica regra que CRUZA dois tipos de evento: dispara num LOGIN_REALIZADO mas
 * inspeciona os LOGIN_FALHADO anteriores da mesma credencial. E a unica que
 * compara IPs — como o IP esta no payload (nao indexado), buscamos a lista de
 * falhas (filtrada por campos indexados) e comparamos os IPs em memoria.
 *
 * Janela de 5 minutos relativa ao timestamp do sucesso.
 */
@Component
public class LoginAposFalhasRule implements RegraAnomalia {

    private static final long JANELA_SEGUNDOS = 300; // 5 minutos
    private static final int MIN_FALHAS_OUTRO_IP = 3;

    private final RegistroAuditoriaRepository repository;

    public LoginAposFalhasRule(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Anomalia> avaliar(RegistroAuditoria evento) {
        if (evento.getTipoEvento() != TipoEvento.LOGIN_REALIZADO
                || evento.getEntidadeAfetada() == null
                || evento.getEntidadeAfetada().getId() == null) {
            return Optional.empty();
        }

        String credencial = evento.getEntidadeAfetada().getId();
        String ipSucesso = lerIp(evento);
        Instant base = evento.getTimestamp() != null ? evento.getTimestamp() : Instant.now();
        Instant inicioJanela = base.minusSeconds(JANELA_SEGUNDOS);

        List<RegistroAuditoria> falhas = repository
                .findByTipoEventoAndEntidadeAfetada_IdAndTimestampGreaterThanEqual(
                        TipoEvento.LOGIN_FALHADO, credencial, inicioJanela);

        if (falhas.isEmpty()) {
            return Optional.empty();
        }

        // Falhas cujo IP e diferente do IP do sucesso.
        List<String> ipsDeOutrosLocais = falhas.stream()
                .map(this::lerIp)
                .filter(ip -> ip != null && !ip.equals(ipSucesso))
                .toList();

        if (ipsDeOutrosLocais.size() >= MIN_FALHAS_OUTRO_IP) {
            Set<String> ipsDistintos = new HashSet<>(ipsDeOutrosLocais);
            Anomalia anomalia = Anomalia.builder()
                    .tipoRegra("LOGIN_APOS_FALHAS_OUTRO_IP")
                    .severidade(SeveridadeAnomalia.CRITICAL)
                    .descricao("Login bem-sucedido da credencial '" + credencial + "' (IP "
                            + ipSucesso + ") apos " + ipsDeOutrosLocais.size()
                            + " falha(s) de outro(s) IP(s) " + ipsDistintos + " em ate 5min")
                    .entidadeRelacionada(credencial)
                    .correlationId(evento.getCorrelationId())
                    .eventoGatilhoId(evento.getEventId())
                    .status(StatusAnomalia.ABERTA)
                    .build();
            return Optional.of(anomalia);
        }
        return Optional.empty();
    }

    private String lerIp(RegistroAuditoria registro) {
        if (registro.getPayload() == null) {
            return null;
        }
        Object ip = registro.getPayload().get("ip");
        return ip != null ? ip.toString() : null;
    }
}