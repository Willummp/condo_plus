package com.condoplus.auditoria.controller;

import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.dto.RegistroAuditoriaRequest;
import com.condoplus.auditoria.dto.RegistroAuditoriaResponse;
import com.condoplus.auditoria.service.AuditoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta de entrada REST do auditoria-service.
 *
 * No TP1, outros servicos (ou scripts curl simulando produtores) publicam
 * eventos por aqui. No TP2 o mesmo fluxo passa a ser alimentado por
 * consumidores Kafka; este endpoint permanece como opcao administrativa.
 */
@RestController
@RequestMapping("/auditoria/registros")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    /**
     * @Valid dispara a Bean Validation do DTO antes de qualquer logica.
     * 201 Created e a resposta semantica para criacao de recurso.
     */
    @PostMapping
    public ResponseEntity<RegistroAuditoriaResponse> registrar(
            @Valid @RequestBody RegistroAuditoriaRequest request) {

        RegistroAuditoria salvo = service.salvar(request.toDomain());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RegistroAuditoriaResponse.fromDomain(salvo));
    }
}