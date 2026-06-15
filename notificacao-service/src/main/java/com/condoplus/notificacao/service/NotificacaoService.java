package com.condoplus.notificacao.service;

import com.condoplus.notificacao.domain.Notificacao;
import com.condoplus.notificacao.domain.StatusNotificacao;
import com.condoplus.notificacao.dto.NotificacaoRequest;
import com.condoplus.notificacao.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public Mono<Notificacao> solicitarNotificacao(NotificacaoRequest request) {
        log.info("Recebendo solicitacao REST para pessoaId={}", request.pessoaId());
        Notificacao nova = new Notificacao();
        nova.setDestinatarioPessoaId(request.pessoaId());
        nova.setEventoOrigemId(request.eventoOrigemId());
        nova.setTipoEvento(request.tipoEvento());
        nova.setCanal(request.canal());
        nova.setTitulo("Aviso do Condomínio");
        nova.setCorpo(request.mensagem());
        nova.setStatus(StatusNotificacao.PENDENTE);
        nova.setTentativas(0);
        nova.setCriadaEm(LocalDateTime.now());
        return notificacaoRepository.save(nova);
    }

    public Flux<Notificacao> listarPorDestinatario(UUID pessoaId) {
        log.debug("Buscando historico para o usuario={}", pessoaId);
        return notificacaoRepository.findByDestinatarioPessoaIdOrderByCriadaEmDesc(pessoaId);
    }

    public Mono<Notificacao> retentar(UUID id) {
        log.info("Solicitando retentativa manual para a notificacao id={}", id);
        return notificacaoRepository.findById(id)
                .flatMap(notif -> {
                    notif.setStatus(StatusNotificacao.PENDENTE);
                    notif.setTentativas(notif.getTentativas() + 1);
                    return notificacaoRepository.save(notif);
                });
    }
}
