package com.condoplus.portaria_service.controller;

import com.condoplus.portaria_service.dto.response.EncomendaResponseDTO;
import com.condoplus.portaria_service.dto.request.NovaEncomendaRequest;
import com.condoplus.portaria_service.dto.request.RetiradaRequest;
import com.condoplus.portaria_service.service.EncomendaService;
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
@RequestMapping("/portaria/encomendas")
@RequiredArgsConstructor
public class EncomendaController {

    private final EncomendaService encomendaService;

    @PostMapping
    @PreAuthorize("hasRole('PORTEIRO')")
    public ResponseEntity<EncomendaResponseDTO> receber(
            @Valid @RequestBody NovaEncomendaRequest req,
            @AuthenticationPrincipal String porteiroIdAsString) {

        UUID porteiroId = UUID.fromString(porteiroIdAsString);
        EncomendaResponseDTO criada = encomendaService.receber(req, porteiroId);

        return ResponseEntity
                .created(URI.create("/portaria/encomendas/" + criada.id()))
                .body(criada);
    }

    @PostMapping("/{id}/retirada")
    @PreAuthorize("hasRole('PORTEIRO')")
    public ResponseEntity<EncomendaResponseDTO> retirar(
            @PathVariable UUID id,
            @Valid @RequestBody RetiradaRequest req,
            @AuthenticationPrincipal String porteiroIdAsString) {

        UUID porteiroId = UUID.fromString(porteiroIdAsString);
        return ResponseEntity.ok(encomendaService.retirar(id, req, porteiroId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EncomendaResponseDTO>> listarPendentes(
            @RequestParam UUID unidadeId) {
        return ResponseEntity.ok(encomendaService.listarPendentesPorUnidade(unidadeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EncomendaResponseDTO> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(encomendaService.buscar(id));
    }
}
