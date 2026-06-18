package com.condoplus.notificacao.controller;

import com.condoplus.notificacao.domain.Notificacao;
import com.condoplus.notificacao.service.NotificacaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/notificacoes")
@Slf4j
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping("/minhas")
    @PreAuthorize("isAuthenticated()")
    public Flux<Notificacao> listarMinhas(
            @AuthenticationPrincipal String pessoaIdAsString) {
        UUID pessoaId = UUID.fromString(pessoaIdAsString);
        return notificacaoService.listarPorDestinatario(pessoaId);
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public Mono<ResponseEntity<Notificacao>> retentarManual(@PathVariable UUID id) {
        return notificacaoService.retentar(id).map(ResponseEntity::ok);
    }
}
