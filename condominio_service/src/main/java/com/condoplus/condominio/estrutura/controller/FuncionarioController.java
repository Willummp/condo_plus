package com.condoplus.condominio.estrutura.controller;

import com.condoplus.condominio.estrutura.domain.CargoFuncionario;
import com.condoplus.condominio.estrutura.dto.FuncionarioResponse;
import com.condoplus.condominio.estrutura.dto.NovoFuncionarioRequest;
import com.condoplus.condominio.estrutura.service.FuncionarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/condominio/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    
    private final FuncionarioService funcionarioService;

    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<FuncionarioResponse> admitir(@Valid @RequestBody NovoFuncionarioRequest req) {
        FuncionarioResponse criado = funcionarioService.admitir(req);
        return ResponseEntity
                .created(URI.create("/condominio/funcionarios/" + criado.id()))
                .body(criado);
    }

    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FuncionarioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'PORTEIRO')")
    public ResponseEntity<List<FuncionarioResponse>> listarTodos(
            @RequestParam(required = false, defaultValue = "true") boolean apenasAtivos,
            @RequestParam(required = false) CargoFuncionario cargo) {
        
        List<FuncionarioResponse> result = cargo != null
                ? funcionarioService.listarPorCargo(cargo)
                : funcionarioService.listarTodos(apenasAtivos);
        return ResponseEntity.ok(result);
    }

    
    @PatchMapping("/{id}/desligar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> desligar(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataDesligamento) {
        
        funcionarioService.desligar(id, dataDesligamento);
        return ResponseEntity.noContent().build();
    }
}
