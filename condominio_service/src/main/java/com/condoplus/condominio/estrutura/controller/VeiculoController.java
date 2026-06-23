package com.condoplus.condominio.estrutura.controller;

import com.condoplus.condominio.estrutura.dto.NovoVeiculoRequest;
import com.condoplus.condominio.estrutura.dto.VeiculoResponse;
import com.condoplus.condominio.estrutura.service.VeiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/condominio/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    
    private final VeiculoService veiculoService;

    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'MORADOR')")
    public ResponseEntity<VeiculoResponse> cadastrar(@Valid @RequestBody NovoVeiculoRequest req) {
        VeiculoResponse criado = veiculoService.cadastrar(req);
        return ResponseEntity
                .created(URI.create("/condominio/veiculos/" + criado.id()))
                .body(criado);
    }

    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VeiculoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'PORTEIRO')")
    public ResponseEntity<List<VeiculoResponse>> listarTodos() {
        return ResponseEntity.ok(veiculoService.listarTodos());
    }

    
    @GetMapping("/unidade/{unidadeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VeiculoResponse>> listarPorUnidade(@PathVariable UUID  unidadeId) {
        return ResponseEntity.ok(veiculoService.listarPorUnidade(unidadeId));
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'MORADOR')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        veiculoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
