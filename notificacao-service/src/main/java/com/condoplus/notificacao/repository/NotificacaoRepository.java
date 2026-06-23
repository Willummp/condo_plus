package com.condoplus.notificacao.repository;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import com.condoplus.notificacao.domain.StatusNotificacao;
import com.condoplus.notificacao.domain.TipoEvento;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface NotificacaoRepository extends ReactiveCrudRepository<Notificacao, UUID> {
    Flux<Notificacao> findByDestinatarioPessoaIdOrderByCriadaEmDesc(UUID pessoaId);
    Mono<Long> countByDestinatarioPessoaIdAndStatus(UUID pessoaId, StatusNotificacao status);

    @Query("SELECT * FROM notificacao.notificacao " +
            "WHERE destinatario_pessoa_id = :pessoaId " +
            "AND evento_origem_id = :eventoOrigemId " +
            "AND canal = :canal")
    Mono<Notificacao> findExistente(UUID pessoaId, String eventoOrigemId, Canal canal);

    @Query("SELECT * FROM notificacao.notificacao " +
            "WHERE status = 'PENDENTE' AND tentativas < :maxTentativas " +
            "ORDER BY criada_em ASC LIMIT :limite")
    Flux<Notificacao> findPendentesPararRetry(int maxTentativas, int limite);
}