package com.condoplus.portaria_service.controller;

import com.condoplus.portaria_service.dto.request.NovoVisitanteRequest;
import com.condoplus.portaria_service.dto.response.VisitanteResponseDTO;
import com.condoplus.portaria_service.service.VisitanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portaria/visitantes")
@RequiredArgsConstructor
public class VisitanteController {

    private final VisitanteService visitanteService;

    /**
     * Qualquer autenticado pode chamar; a regra de PRESTADOR→SÍNDICO
     * é verificada dentro do service, pois é condicional ao tipo.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VisitanteResponseDTO> autorizar(
            @Valid @RequestBody NovoVisitanteRequest req,
            @AuthenticationPrincipal String pessoaIdAsString) {

        UUID pessoaId = UUID.fromString(pessoaIdAsString);
        VisitanteResponseDTO visitante = visitanteService.autorizar(req, pessoaId);

        return ResponseEntity
                .created(URI.create("/portaria/visitantes/" + visitante.id()))
                .body(visitante);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VisitanteResponseDTO>> listarPorUnidade(
            @RequestParam UUID unidadeId) {
        return ResponseEntity.ok(visitanteService.listarPorUnidade(unidadeId));
    }

    @PatchMapping("/{id}/encerrar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> encerrar(@PathVariable UUID id) {
        visitanteService.encerrar(id);
        return ResponseEntity.noContent().build();
    }
}
