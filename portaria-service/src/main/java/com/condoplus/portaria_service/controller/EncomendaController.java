package com.condoplus.portaria_service.controller;

import com.condoplus.portaria_service.dto.CriarEncomendaDTO;
import com.condoplus.portaria_service.dto.EncomendaResponseDTO;
import com.condoplus.portaria_service.dto.RetirarEncomendaDTO;
import com.condoplus.portaria_service.service.EncomendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portaria/encomendas")
@RequiredArgsConstructor
public class EncomendaController {

    private final EncomendaService encomendaService;

    @PostMapping
    public ResponseEntity<EncomendaResponseDTO> criar(@Valid @RequestBody CriarEncomendaDTO dto) {
        return ResponseEntity.ok(encomendaService.registrarEncomenda(dto));
    }

    @PostMapping("/retirada")
    public ResponseEntity<Void> retirar(@Valid @RequestBody RetirarEncomendaDTO dto) {
        encomendaService.retirarEncomenda(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unidade/{unidadeId}")
    public ResponseEntity<List<EncomendaResponseDTO>> listarPendentes(@PathVariable UUID unidadeId) {
        return ResponseEntity.ok(encomendaService.listarPendentesPorUnidade(unidadeId));
    }
}

