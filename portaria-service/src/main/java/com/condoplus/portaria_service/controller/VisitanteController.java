package com.condoplus.portaria_service.controller;

import com.condoplus.portaria_service.dto.CriarVisitanteDTO;
import com.condoplus.portaria_service.dto.RegistrarEntradaVisitanteDTO;
import com.condoplus.portaria_service.dto.VisitanteResponseDTO;
import com.condoplus.portaria_service.service.VisitanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portaria/visitantes")
@RequiredArgsConstructor
public class VisitanteController {

    private final VisitanteService visitanteService;

    @PostMapping
    public ResponseEntity<VisitanteResponseDTO> criar(@Valid @RequestBody CriarVisitanteDTO dto) {
        return ResponseEntity.ok(visitanteService.criarVisitante(dto));
    }

    @PostMapping("/entrada")
    public ResponseEntity<Void> registrarEntrada(@Valid @RequestBody RegistrarEntradaVisitanteDTO dto) {
        visitanteService.registrarEntradaVisitante(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unidade/{unidadeId}")
    public ResponseEntity<List<VisitanteResponseDTO>> listarPorUnidade(@PathVariable UUID unidadeId) {
        return ResponseEntity.ok(visitanteService.listarPorUnidade(unidadeId));
    }
}