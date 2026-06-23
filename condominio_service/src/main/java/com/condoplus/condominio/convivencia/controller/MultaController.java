package com.condoplus.condominio.convivencia.controller;

import com.condoplus.condominio.convivencia.domain.StatusMulta;
import com.condoplus.condominio.convivencia.dto.MultaResponse;
import com.condoplus.condominio.convivencia.dto.NovaMultaRequest;
import com.condoplus.condominio.convivencia.service.MultaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/condominio/multas")
@RequiredArgsConstructor
public class MultaController {

    private final MultaService multaService;

    
    @PostMapping
    @PreAuthorize("hasRole('SINDICO')")
    public ResponseEntity<MultaResponse> aplicar(
            @Valid @RequestBody NovaMultaRequest req,
            Authentication auth) {
        UUID aplicadorId = UUID.fromString(auth.getName());
        MultaResponse criada = multaService.aplicar(req, aplicadorId);
        return ResponseEntity
                .created(URI.create("/condominio/multas/" + criada.id()))
                .body(criada);
    }

    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MultaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(multaService.buscarPorId(id));
    }

    
    @GetMapping("/unidade/{unidadeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MultaResponse>> listarPorUnidade(
            @PathVariable UUID unidadeId,
            @RequestParam(required = false) StatusMulta status) {
        return ResponseEntity.ok(multaService.listarPorUnidade(unidadeId, status));
    }

    
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<MultaResponse> atualizarStatus(
            @PathVariable UUID id,
            @RequestParam StatusMulta status) {
        return ResponseEntity.ok(multaService.atualizarStatus(id, status));
    }
}
