package com.condoplus.condominio.estrutura.controller;

import com.condoplus.condominio.estrutura.dto.NovaPessoaRequest;
import com.condoplus.condominio.estrutura.dto.PessoaResponse;
import com.condoplus.condominio.estrutura.service.PessoaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/condominio/pessoas")
@RequiredArgsConstructor
public class PessoaController {

    private final PessoaService pessoaService;

    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<PessoaResponse> cadastrar(@Valid @RequestBody NovaPessoaRequest req) {
        PessoaResponse criada = pessoaService.cadastrar(req);
        return ResponseEntity
                .created(URI.create("/condominio/pessoas/" + criada.id()))
                .body(criada);
    }

    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PessoaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pessoaService.buscarPorId(id));
    }

    
    @GetMapping(params = "cpf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'PORTEIRO')")
    public ResponseEntity<PessoaResponse> buscarPorCpf(@RequestParam String cpf) {
        return ResponseEntity.ok(pessoaService.buscarPorCpf(cpf));
    }

    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<List<PessoaResponse>> listarTodas() {
        return ResponseEntity.ok(pessoaService.listarTodas());
    }
}
