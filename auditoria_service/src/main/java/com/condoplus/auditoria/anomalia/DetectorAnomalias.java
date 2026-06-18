package com.condoplus.auditoria.anomalia;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.repository.AnomaliaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Roda todas as regras contra cada evento e persiste as anomalias detectadas.
 *
 * O Spring injeta automaticamente TODAS as beans que implementam RegraAnomalia
 * na lista 'regras'. Adicionar uma regra nova nao mexe nesta classe (Open/Closed).
 *
 * Observabilidade (C23): cada anomalia detectada incrementa um contador
 * Micrometer rotulado por tipoRegra e severidade. Exposto em /actuator/prometheus,
 * permite responder "quantas anomalias CRITICAL do tipo X em tal periodo".
 */
@Component
public class DetectorAnomalias {

    private static final Logger log = LoggerFactory.getLogger(DetectorAnomalias.class);

    private final List<RegraAnomalia> regras;
    private final AnomaliaRepository anomaliaRepository;
    private final MeterRegistry meterRegistry;

    public DetectorAnomalias(List<RegraAnomalia> regras, AnomaliaRepository anomaliaRepository,
                             MeterRegistry meterRegistry) {
        this.regras = regras;
        this.anomaliaRepository = anomaliaRepository;
        this.meterRegistry = meterRegistry;
        log.info("DetectorAnomalias iniciado com {} regra(s)", regras.size());
    }

    public void avaliar(RegistroAuditoria evento) {
        for (RegraAnomalia regra : regras) {
            regra.avaliar(evento).ifPresent(anomalia -> {
                anomaliaRepository.save(anomalia);
                registrarMetrica(anomalia);
                log.warn("ANOMALIA detectada [{}] severidade={} entidade={} -> {}",
                        anomalia.getTipoRegra(), anomalia.getSeveridade(),
                        anomalia.getEntidadeRelacionada(), anomalia.getDescricao());
            });
        }
    }

    /**
     * Incrementa o contador 'auditoria_anomalias_detectadas_total' com tags
     * tipoRegra e severidade. Tags viram dimensoes consultaveis no Prometheus.
     */
    private void registrarMetrica(Anomalia anomalia) {
        Counter.builder("auditoria.anomalias.detectadas")
                .description("Total de anomalias detectadas pelo auditoria-service")
                .tag("tipoRegra", anomalia.getTipoRegra())
                .tag("severidade", anomalia.getSeveridade().name())
                .register(meterRegistry)
                .increment();
    }
}