package com.condoplus.condominio.convivencia.controller;

import com.condoplus.condominio.convivencia.dto.ComunicadoResponse;
import com.condoplus.condominio.convivencia.dto.NovoComunicadoRequest;
import com.condoplus.condominio.convivencia.service.ComunicadoService;
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
@RequestMapping("/condominio/comunicados")
@RequiredArgsConstructor
public class ComunicadoController {

    private final ComunicadoService comunicadoService;

    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<ComunicadoResponse> publicar(
            @Valid @RequestBody NovoComunicadoRequest req,
            Authentication auth) {
        UUID autorId = UUID.fromString(auth.getName());
        ComunicadoResponse criado = comunicadoService.publicar(req, autorId);
        return ResponseEntity
                .created(URI.create("/condominio/comunicados/" + criado.id()))
                .body(criado);
    }

    
    @PostMapping("/bloco/{bloco}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<ComunicadoResponse> publicarParaBloco(
            @Valid @RequestBody NovoComunicadoRequest req,
            @PathVariable String bloco,
            Authentication auth) {
        UUID autorId = UUID.fromString(auth.getName());
        ComunicadoResponse criado = comunicadoService.publicarParaBloco(req, autorId, bloco);
        return ResponseEntity
                .created(URI.create("/condominio/comunicados/" + criado.id()))
                .body(criado);
    }

    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComunicadoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(comunicadoService.buscarPorId(id));
    }

    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComunicadoResponse>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(comunicadoService.listarTodos(page, size));
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        comunicadoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
