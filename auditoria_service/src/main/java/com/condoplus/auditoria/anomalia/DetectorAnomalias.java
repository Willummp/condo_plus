package com.condoplus.auditoria.anomalia;

import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.repository.AnomaliaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Roda todas as regras contra cada evento e persiste as anomalias detectadas.
 *
 * O Spring injeta automaticamente TODAS as beans que implementam RegraAnomalia
 * na lista 'regras'. Por isso adicionar uma regra nova nao mexe nesta classe.
 */
@Component
public class DetectorAnomalias {

    private static final Logger log = LoggerFactory.getLogger(DetectorAnomalias.class);

    private final List<RegraAnomalia> regras;
    private final AnomaliaRepository anomaliaRepository;

    public DetectorAnomalias(List<RegraAnomalia> regras, AnomaliaRepository anomaliaRepository) {
        this.regras = regras;
        this.anomaliaRepository = anomaliaRepository;
        log.info("DetectorAnomalias iniciado com {} regra(s)", regras.size());
    }

    public void avaliar(RegistroAuditoria evento) {
        for (RegraAnomalia regra : regras) {
            regra.avaliar(evento).ifPresent(anomalia -> {
                anomaliaRepository.save(anomalia);
                log.warn("ANOMALIA detectada [{}] severidade={} entidade={} -> {}",
                        anomalia.getTipoRegra(), anomalia.getSeveridade(),
                        anomalia.getEntidadeRelacionada(), anomalia.getDescricao());
            });
        }
    }
}