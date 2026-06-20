package com.condoplus.auditoria.controller;

import com.condoplus.auditoria.domain.StatusAnomalia;
import com.condoplus.auditoria.dto.AnomaliaResponse;
import com.condoplus.auditoria.dto.AtualizarStatusRequest;
import com.condoplus.auditoria.service.AnomaliaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.condoplus.auditoria.domain.Anomalia;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/auditoria/anomalias")
public class AnomaliaController {

    private final AnomaliaService service;

    public AnomaliaController(AnomaliaService service) {
        this.service = service;
    }

    /**
     * Lista anomalias, mais recentes primeiro, com filtro opcional por status.
     * Ex: GET /auditoria/anomalias?status=ABERTA&page=0&size=20
     */
    @GetMapping
    public Page<AnomaliaResponse> listar(
            @RequestParam(required = false) StatusAnomalia status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectadaEm"));
        return service.listar(status, pageable).map(AnomaliaResponse::de);
    }

    /**
     * Atualiza o status de uma anomalia (triagem do sindico).
     * Ex: PATCH /auditoria/anomalias/{id}/status  body: {"status":"RECONHECIDA"}
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AnomaliaResponse> atualizarStatus(
            @PathVariable String id,
            @Valid @RequestBody AtualizarStatusRequest request) {
        Anomalia atualizada = service.atualizarStatus(id, request.status());
        return ResponseEntity.ok(AnomaliaResponse.de(atualizada));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> naoEncontrada(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}