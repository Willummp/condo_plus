package com.condoplus.portaria_service.service;

import com.condoplus.portaria_service.model.entities.Encomenda;
import com.condoplus.portaria_service.model.enums.StatusEncomenda;
import com.condoplus.portaria_service.model.enums.TipoEncomenda;
import com.condoplus.portaria_service.repository.EncomendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EncomendaExpiracaoJob {

    private final EncomendaRepository encomendaRepository;
    private final EncomendaRedisStore redisStore;

    @Scheduled(cron = "${condoplus.portaria.expiracao-job.cron}")
    @Transactional
    public void marcarExpiradas() {
        List<Encomenda> aguardando = encomendaRepository
                .findByTipoAndStatus(TipoEncomenda.CURTO_PRAZO, StatusEncomenda.AGUARDANDO_RETIRADA);

        int marcadas = 0;
        for (Encomenda e : aguardando) {
            boolean aindaAtiva = redisStore.estaAtiva(e.getUnidadeId(), e.getId());
            if (!aindaAtiva) {
                e.setStatus(StatusEncomenda.EXPIRADA);
                marcadas++;
                log.info("Encomenda CURTO_PRAZO expirada pelo job. id={} unidadeId={}",
                        e.getId(), e.getUnidadeId());
            }
        }

        if (marcadas > 0) {
            log.info("Job de expiração concluído. encomendas marcadas={}", marcadas);
        }
    }
}
