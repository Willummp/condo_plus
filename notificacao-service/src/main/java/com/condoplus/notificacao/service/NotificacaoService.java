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
        log.info("Recebendo solicitacao de notificacao REST para pessoaId={}", request.pessoaId());

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

    public Flux<Notificacao> buscarPorPessoa(UUID pessoaId) {
        log.debug("Buscando historico de notificacoes para pessoaId={}", pessoaId);
        return notificacaoRepository.findByDestinatarioPessoaIdOrderByCriadaEmDesc(pessoaId);
    }
}