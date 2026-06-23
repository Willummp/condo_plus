package com.condoplus.condominio.estrutura.controller;

import com.condoplus.condominio.estrutura.dto.*;
import com.condoplus.condominio.estrutura.service.UnidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST responsável por expor os endpoints de gerenciamento de Unidades e suas Vinculações.
 * 
 * <p>Este controlador atua como a camada web do bounded context de Estrutura, delegando
 * toda a regra de negócio para a camada de serviço (UnidadeService).
 */
@RestController
@RequestMapping("/condominio/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService unidadeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<UnidadeResponse> criar(@Valid @RequestBody NovaUnidadeRequest req) {
        UnidadeResponse salva = unidadeService.criar(req);
        return ResponseEntity
            .created(URI.create("/condominio/unidades/" + salva.id()))
            .body(salva);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnidadeResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(unidadeService.buscar(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UnidadeResponse>> listar(
            @RequestParam(required = false) String bloco) {
        return ResponseEntity.ok(unidadeService.listar(bloco));
    }

    @PostMapping("/{unidadeId}/vinculacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<VinculacaoResponse> vincular(
            @PathVariable UUID unidadeId,
            @Valid @RequestBody NovaVinculacaoRequest req) {
        VinculacaoResponse criada = unidadeService.vincular(unidadeId, req);
        return ResponseEntity.status(201).body(criada);
    }

    @PatchMapping("/vinculacoes/{vinculacaoId}/encerrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> encerrarVinculacao(@PathVariable UUID vinculacaoId) {
        unidadeService.encerrarVinculacao(vinculacaoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/vinculacoes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VinculacaoResponse>> listarVinculacoes(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivas) {
        return ResponseEntity.ok(unidadeService.listarVinculacoes(id, apenasAtivas));
    }
}
