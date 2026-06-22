package com.condoplus.notificacao.service;

import com.condoplus.notificacao.client.CondominioWebClient;
import com.condoplus.notificacao.domain.TipoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Service
public class ResolverDestinatariosService {

    private static final Logger log = LoggerFactory.getLogger(ResolverDestinatariosService.class);
    private final CondominioWebClient condominioClient;

    public ResolverDestinatariosService(CondominioWebClient condominioClient) {
        this.condominioClient = condominioClient;
    }

    public Flux<UUID> resolverDestinatarios(EventoNotificacao evento) {
        if (evento.pessoaIdEspecifica() != null) {
            return Flux.just(evento.pessoaIdEspecifica());
        }

        if (evento.unidadeId() != null) {
            return condominioClient.listarPessoasDaUnidade(evento.unidadeId())
                    .doOnNext(pid -> log.trace("Destinatário resolvido: {}", pid))
                    .onErrorResume(ex -> {
                        log.warn("Falha ao resolver destinatários da unidade {}. erro={}",
                                evento.unidadeId(), ex.getMessage());
                        return Flux.empty();
                    });
        }

        if (evento.tipoEvento() == TipoEvento.COMUNICADO_PUBLICADO) {
            return condominioClient.listarTodosMoradoresAtivos()
                    .onErrorResume(ex -> {
                        log.warn("Falha ao resolver destinatários para COMUNICADO_PUBLICADO. erro={}",
                                ex.getMessage());
                        return Flux.empty();
                    });
        }

        log.warn("Evento sem destinatário identificável: {}", evento.eventoOrigemId());
        return Flux.empty();
    }
}
