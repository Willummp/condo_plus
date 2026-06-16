package com.condoplus.auditoria.service;

import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.repository.RegistroAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Servico de aplicacao do auditoria-service.
 *
 * Responsavel por persistir registros de auditoria de forma idempotente
 * e (no C8) por consultar o historico.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final RegistroAuditoriaRepository repository;

    // Injecao via construtor (preferida a @Autowired em campo):
    // deixa a dependencia explicita, facilita teste e permite campo final.
    public AuditoriaService(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    /**
     * Persiste um registro de auditoria de forma IDEMPOTENTE.
     *
     * A garantia real de unicidade e o indice unique em eventId (C4).
     * Tentamos salvar direto; se o mesmo eventId ja existe, o MongoDB
     * recusa e o driver lanca DuplicateKeyException. Capturamos e
     * tratamos como SUCESSO SILENCIOSO — devolvemos o registro que ja
     * estava la, sem criar duplicata e sem quebrar o fluxo.
     *
     * Por que nao checar antes com findByEventId?
     * Checar-depois-salvar tem condicao de corrida: sob redelivery
     * simultaneo do Kafka (TP2), dois processos podem checar "nao existe"
     * ao mesmo tempo e ambos tentar salvar. So a constraint do banco
     * resolve isso. A constraint e a fonte da verdade; o catch e a
     * rede de protecao.
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
            // Devolve o registro original que ja estava persistido.
            return repository.findByEventId(registro.getEventId())
                    .orElseThrow(() -> e); // se sumiu entre o erro e a busca, repropaga
        }
    }
}