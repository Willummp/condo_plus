package com.condoplus.notificacao.service;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import com.condoplus.notificacao.domain.StatusNotificacao;
import com.condoplus.notificacao.dto.NotificacaoRequest;
import com.condoplus.notificacao.repository.NotificacaoRepository;
import com.condoplus.notificacao.repository.PreferenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoRepository notificacaoRepository;
    private final PreferenciaRepository preferenciaRepository;
    private final ResolverDestinatariosService resolverService;
    private final DespacharService despacharService;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              PreferenciaRepository preferenciaRepository,
                              ResolverDestinatariosService resolverService,
                              DespacharService despacharService) {
        this.notificacaoRepository = notificacaoRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.resolverService = resolverService;
        this.despacharService = despacharService;
    }

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

    public Flux<Notificacao> processarEvento(EventoNotificacao evento) {
        log.info("Processando evento de notificação. tipo={} origemId={}",
                evento.tipoEvento(), evento.eventoOrigemId());
        return resolverService.resolverDestinatarios(evento)
                .flatMap(pessoaId -> processarParaDestinatario(evento, pessoaId))
                .doOnComplete(() -> log.debug("Fan-out completo para evento {}",
                        evento.eventoOrigemId()));
    }

    private Flux<Notificacao> processarParaDestinatario(EventoNotificacao evento, UUID pessoaId) {
        return preferenciaRepository
                .findByPessoaIdAndTipoEventoAndAtivaTrue(pessoaId, evento.tipoEvento())
                .flatMap(pref -> criarOuRecuperarNotificacao(evento, pessoaId, pref.getCanal()))
                .flatMap(notif -> despacharSeNova(notif));
    }

    @Transactional
    public Mono<Notificacao> criarOuRecuperarNotificacao(EventoNotificacao evento, UUID pessoaId, Canal canal) {
        Notificacao nova = new Notificacao();
        nova.setDestinatarioPessoaId(pessoaId);
        nova.setTipoEvento(evento.tipoEvento());
        nova.setEventoOrigemId(evento.eventoOrigemId());
        nova.setCanal(canal);
        nova.setTitulo(evento.titulo());
        nova.setCorpo(evento.corpo());
        nova.setStatus(StatusNotificacao.PENDENTE);
        nova.setTentativas(0);
        nova.setCriadaEm(LocalDateTime.now());

        return notificacaoRepository.save(nova)
                .onErrorResume(DataIntegrityViolationException.class, ex -> {
                    log.debug("Notificação duplicada detectada (idempotência). " +
                                    "pessoaId={} eventoOrigemId={} canal={}",
                            pessoaId, evento.eventoOrigemId(), canal);
                    return notificacaoRepository.findExistente(
                            pessoaId, evento.eventoOrigemId(), canal);
                });
    }

    private Mono<Notificacao> despacharSeNova(Notificacao notif) {
        if (notif.getStatus() != StatusNotificacao.PENDENTE) {
            log.debug("Notificação já processada anteriormente. id={} status={}",
                    notif.getId(), notif.getStatus());
            return Mono.just(notif);
        }
        return despacharService.despachar(notif);
    }
}
