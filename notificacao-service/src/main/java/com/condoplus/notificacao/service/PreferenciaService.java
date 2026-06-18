package com.condoplus.notificacao.service;

import com.condoplus.notificacao.domain.PreferenciaNotificacao;
import com.condoplus.notificacao.dto.AtualizarPreferenciaRequest;
import com.condoplus.notificacao.repository.PreferenciaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PreferenciaService {

    private final PreferenciaRepository preferenciaRepository;

    public PreferenciaService(PreferenciaRepository preferenciaRepository) {
        this.preferenciaRepository = preferenciaRepository;
    }

    public Mono<PreferenciaNotificacao> atualizar(UUID pessoaId, AtualizarPreferenciaRequest request) {
        return preferenciaRepository.findByPessoaIdAndTipoEventoAndCanal(
                        pessoaId, request.tipoEvento(), request.canal()
                )
                .flatMap(prefExistente -> {
                    prefExistente.setAtiva(request.ativa());
                    prefExistente.setAtualizadaEm(LocalDateTime.now());
                    return preferenciaRepository.save(prefExistente);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    PreferenciaNotificacao novaPref = new PreferenciaNotificacao();
                    novaPref.setPessoaId(pessoaId);
                    novaPref.setTipoEvento(request.tipoEvento());
                    novaPref.setCanal(request.canal());
                    novaPref.setAtiva(request.ativa());
                    novaPref.setCriadaEm(LocalDateTime.now());
                    novaPref.setAtualizadaEm(LocalDateTime.now());
                    return preferenciaRepository.save(novaPref);
                }));
    }

    public Flux<PreferenciaNotificacao> listarPorPessoa(UUID pessoaId) {
        return preferenciaRepository.findByPessoaId(pessoaId);
    }
}
