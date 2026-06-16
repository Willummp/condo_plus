package com.condoplus.auditoria.service;

import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.TipoEvento;
import com.condoplus.auditoria.repository.RegistroAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final RegistroAuditoriaRepository repository;

    public AuditoriaService(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    /**
     * Salva de forma idempotente. A trava real e o indice unique de eventId
     * (C4): tenta salvar direto; se duplicar, o Mongo recusa com
     * DuplicateKeyException, que tratamos como sucesso devolvendo o que ja existia.
     */
    public RegistroAuditoria salvar(RegistroAuditoria registro) {
        try {
            RegistroAuditoria salvo = repository.save(registro);
            log.info("Registro de auditoria persistido: eventId={}, tipo={}",
                    salvo.getEventId(), salvo.getTipoEvento());
            return salvo;
        } catch (DuplicateKeyException e) {
            log.warn("Evento duplicado ignorado (idempotencia): eventId={}",
                    registro.getEventId());
            return repository.findByEventId(registro.getEventId())
                    .orElseThrow(() -> e);
        }
    }

    /** Lista paginada; se tipoEvento for informado, filtra por ele. */
    public Page<RegistroAuditoria> listar(TipoEvento tipoEvento, Pageable pageable) {
        if (tipoEvento != null) {
            return repository.findByTipoEvento(tipoEvento, pageable);
        }
        return repository.findAllByOrderByTimestampDesc(pageable);
    }

    /** Historico de uma entidade, do mais recente ao mais antigo. */
    public List<RegistroAuditoria> historicoDaEntidade(String tipoEntidade, String idEntidade) {
        return repository
                .findByEntidadeAfetada_TipoAndEntidadeAfetada_IdOrderByTimestampDesc(
                        tipoEntidade, idEntidade);
    }
}