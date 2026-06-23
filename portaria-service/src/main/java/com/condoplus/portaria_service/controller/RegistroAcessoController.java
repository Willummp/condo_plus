package com.condoplus.portaria_service.controller;

import com.condoplus.portaria_service.dto.request.EntradaMoradorRequest;
import com.condoplus.portaria_service.dto.request.EntradaVisitanteRequest;
import com.condoplus.portaria_service.dto.response.RegistroAcessoResponseDTO;
import com.condoplus.portaria_service.dto.request.SaidaRequest;
import com.condoplus.portaria_service.service.RegistroAcessoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/portaria/acessos")
@RequiredArgsConstructor
public class RegistroAcessoController {

    private final RegistroAcessoService registroService;

    @PostMapping("/entrada/morador")
    @PreAuthorize("hasRole('PORTEIRO')")
    public ResponseEntity<RegistroAcessoResponseDTO> entradaMorador(
            @Valid @RequestBody EntradaMoradorRequest req,
            @AuthenticationPrincipal String porteiroIdAsString) {

        UUID porteiroId = UUID.fromString(porteiroIdAsString);
        return ResponseEntity.status(201)
                .body(registroService.registrarEntradaMorador(req, porteiroId));
    }

    @PostMapping("/entrada/visitante")
    @PreAuthorize("hasRole('PORTEIRO')")
    public ResponseEntity<RegistroAcessoResponseDTO> entradaVisitante(
            @Valid @RequestBody EntradaVisitanteRequest req,
            @AuthenticationPrincipal String porteiroIdAsString) {

        UUID porteiroId = UUID.fromString(porteiroIdAsString);
        return ResponseEntity.status(201)
                .body(registroService.registrarEntradaVisitante(req, porteiroId));
    }

    @PostMapping("/saida")
    @PreAuthorize("hasRole('PORTEIRO')")
    public ResponseEntity<RegistroAcessoResponseDTO> saida(
            @Valid @RequestBody SaidaRequest req,
            @AuthenticationPrincipal String porteiroIdAsString) {

        UUID porteiroId = UUID.fromString(porteiroIdAsString);
        return ResponseEntity.status(201)
                .body(registroService.registrarSaida(req, porteiroId));
    }
}
