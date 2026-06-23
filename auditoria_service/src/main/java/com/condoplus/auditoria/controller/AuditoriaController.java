package com.condoplus.auditoria.controller;

import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.TipoEvento;
import com.condoplus.auditoria.dto.RegistroAuditoriaRequest;
import com.condoplus.auditoria.dto.RegistroAuditoriaResponse;
import com.condoplus.auditoria.service.AuditoriaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auditoria/registros")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    /** Recebe e persiste um evento de forma idempotente. */
    @PostMapping
    public ResponseEntity<RegistroAuditoriaResponse> registrar(
            @Valid @RequestBody RegistroAuditoriaRequest request) {
        RegistroAuditoria salvo = service.salvar(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegistroAuditoriaResponse.fromDomain(salvo));
    }

    /**
     * Lista registros, do mais recente ao mais antigo, paginado.
     * Filtro opcional: ?tipoEvento=MULTA_APLICADA&page=0&size=20
     */
    @GetMapping
    public Page<RegistroAuditoriaResponse> listar(
            @RequestParam(required = false) TipoEvento tipoEvento,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(tipoEvento, pageable)
                .map(RegistroAuditoriaResponse::fromDomain);
    }

    /**
     * Historico de uma entidade especifica.
     * Ex: /auditoria/registros/historico?tipoEntidade=Multa&idEntidade=multa-42
     */
    @GetMapping("/historico")
    public List<RegistroAuditoriaResponse> historicoDaEntidade(
            @RequestParam String tipoEntidade,
            @RequestParam String idEntidade) {
        return service.historicoDaEntidade(tipoEntidade, idEntidade).stream()
                .map(RegistroAuditoriaResponse::fromDomain)
                .toList();
    }
}