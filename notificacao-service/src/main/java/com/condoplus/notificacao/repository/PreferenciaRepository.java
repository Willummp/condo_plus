package com.condoplus.notificacao.repository;
import com.condoplus.notificacao.domain.PreferenciaNotificacao;
import com.condoplus.notificacao.domain.TipoEvento;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface PreferenciaRepository extends ReactiveCrudRepository<PreferenciaNotificacao, UUID> {
    Flux<PreferenciaNotificacao> findByPessoaIdAndTipoEventoAndAtivaTrue(
            UUID pessoaId, TipoEvento tipoEvento);

    Flux<PreferenciaNotificacao> findByPessoaId(UUID pessoaId);
    Mono<PreferenciaNotificacao> findByPessoaIdAndTipoEventoAndCanal(
            UUID pessoaId, TipoEvento tipoEvento,
            com.condoplus.notificacao.domain.Canal canal);
}
