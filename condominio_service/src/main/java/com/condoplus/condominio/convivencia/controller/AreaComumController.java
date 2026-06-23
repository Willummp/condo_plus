package com.condoplus.condominio.convivencia.controller;

import com.condoplus.condominio.convivencia.dto.AreaComumResponse;
import com.condoplus.condominio.convivencia.dto.NovaAreaComumRequest;
import com.condoplus.condominio.convivencia.service.AreaComumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/condominio/areas-comuns")
@RequiredArgsConstructor
public class AreaComumController {

    private final AreaComumService areaComumService;

    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<AreaComumResponse> cadastrar(@Valid @RequestBody NovaAreaComumRequest req) {
        AreaComumResponse criada = areaComumService.cadastrar(req);
        return ResponseEntity
                .created(URI.create("/condominio/areas-comuns/" + criada.id()))
                .body(criada);
    }

    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AreaComumResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(areaComumService.buscarPorId(id));
    }

    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AreaComumResponse>> listarTodas(
            @RequestParam(required = false, defaultValue = "true") boolean apenasAtivas) {
        return ResponseEntity.ok(areaComumService.listarTodas(apenasAtivas));
    }

    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<AreaComumResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody NovaAreaComumRequest req) {
        return ResponseEntity.ok(areaComumService.atualizar(id, req));
    }

    
    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        areaComumService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    
    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> ativar(@PathVariable UUID id) {
        areaComumService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
