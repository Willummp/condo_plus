package com.condoplus.auditoria.repository;

import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.TipoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository do documento RegistroAuditoria.
 *
 * Estende MongoRepository, que ja entrega save, findById, findAll,
 * deleteById, count, etc. de graca. Nao escrevemos implementacao:
 * o Spring Data gera o codigo em runtime lendo o NOME de cada metodo
 * (derived queries).
 *
 * Os metodos abaixo foram desenhados para casar com os indices criados
 * no C4 — cada consulta usa um indice existente, sem table scan.
 */
@Repository
public interface RegistroAuditoriaRepository
        extends MongoRepository<RegistroAuditoria, String> {

    /**
     * Busca por eventId. Usado na checagem de IDEMPOTENCIA (C6).
     * Usa o indice unique de eventId criado no C4.
     */
    Optional<RegistroAuditoria> findByEventId(String eventId);

    /**
     * Eventos de um servico numa janela de tempo.
     * Casa com o indice composto idx_servico_timestamp (servicoOrigem, timestamp DESC).
     */
    List<RegistroAuditoria> findByServicoOrigemAndTimestampBetween(
            String servicoOrigem, Instant inicio, Instant fim);

    /**
     * Historico de uma entidade em ordem cronologica reversa.
     * Casa com o indice composto idx_entidade_timestamp.
     * O underscore (EntidadeAfetada_Tipo) diz ao Spring Data para
     * navegar no subdocumento entidadeAfetada.tipo / entidadeAfetada.id.
     */
    List<RegistroAuditoria> findByEntidadeAfetada_TipoAndEntidadeAfetada_IdOrderByTimestampDesc(
            String tipo, String id);

    /**
     * Filtro por tipo de evento, paginado (para o GET /logs do C8).
     */
    Page<RegistroAuditoria> findByTipoEvento(TipoEvento tipoEvento, Pageable pageable);

    /**
     * Listagem geral paginada, mais recentes primeiro.
     */
    Page<RegistroAuditoria> findAllByOrderByTimestampDesc(Pageable pageable);
}