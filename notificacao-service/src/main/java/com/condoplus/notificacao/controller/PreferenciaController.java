package com.condoplus.notificacao.controller;

import com.condoplus.notificacao.domain.PreferenciaNotificacao;
import com.condoplus.notificacao.dto.AtualizarPreferenciaRequest;
import com.condoplus.notificacao.service.PreferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/notificacoes/preferencias")
@RequiredArgsConstructor
public class PreferenciaController {

    private final PreferenciaService preferenciaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Flux<PreferenciaNotificacao> listarMinhas(
            @AuthenticationPrincipal String pessoaIdAsString) {
        UUID pessoaId = UUID.fromString(pessoaIdAsString);
        return preferenciaService.listarPorPessoa(pessoaId);
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<PreferenciaNotificacao>> atualizar(
            @Valid @RequestBody AtualizarPreferenciaRequest req,
            @AuthenticationPrincipal String pessoaIdAsString) {
        UUID pessoaId = UUID.fromString(pessoaIdAsString);
        return preferenciaService.atualizar(pessoaId, req)
                .map(ResponseEntity::ok);
    }
}
