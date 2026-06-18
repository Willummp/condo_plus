package com.condoplus.notificacao.service;

import com.condoplus.notificacao.client.CondominioWebClient;
import com.condoplus.notificacao.domain.TipoEvento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResolverDestinatariosService {

    private final CondominioWebClient condominioClient;

    public Flux<UUID> resolverDestinatarios(EventoNotificacao evento) {
        if (evento.pessoaIdEspecifica() != null) {
            return Flux.just(evento.pessoaIdEspecifica());
        }

        if (evento.unidadeId() != null) {
            return condominioClient.listarPessoasDaUnidade(evento.unidadeId())
                    .doOnNext(pid -> log.trace("Destinatário resolvido: {}", pid))
                    .onErrorResume(ex -> {
                        log.warn("Falha ao resolver destinatários da unidade {}. " +
                                        "Notificação não será enviada. erro={}",
                                evento.unidadeId(), ex.getMessage());
                        return Flux.empty();
                    });
        }

        if (evento.tipoEvento() == TipoEvento.COMUNICADO_PUBLICADO) {
            return condominioClient.listarTodosMoradoresAtivos();
        }

        log.warn("Evento sem destinatário identificável: {}", evento.eventoOrigemId());
        return Flux.empty();
    }
}
