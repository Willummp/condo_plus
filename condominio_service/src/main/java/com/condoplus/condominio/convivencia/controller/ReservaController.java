package com.condoplus.condominio.convivencia.controller;

import com.condoplus.condominio.convivencia.dto.NovaReservaRequest;
import com.condoplus.condominio.convivencia.dto.ReservaResponse;
import com.condoplus.condominio.convivencia.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/condominio/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> criar(
            @Valid @RequestBody NovaReservaRequest req,
            Authentication auth) {
        UUID moradorId = UUID.fromString(auth.getName());
        ReservaResponse criada = reservaService.criar(req, moradorId);
        return ResponseEntity
            .created(URI.create("/condominio/reservas/" + criada.id()))
            .body(criada);
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id, Authentication auth) {
        UUID moradorId = UUID.fromString(auth.getName());
        reservaService.cancelar(id, moradorId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaResponse>> listar(
            @RequestParam(required = false) UUID areaComumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Authentication auth) {
        if (areaComumId != null && data != null) {
            return ResponseEntity.ok(reservaService.listar(areaComumId, data));
        }
        // sem filtros: retorna reservas do morador logado
        UUID moradorId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(reservaService.listarPorMorador(moradorId));
    }
}
