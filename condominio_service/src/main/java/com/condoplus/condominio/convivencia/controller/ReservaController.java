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

/**
 * Controller responsável por expor os endpoints REST de agendamento (reservas) de Áreas Comuns.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @RestController} — Identifica a classe como um controlador Spring REST, onde as respostas dos métodos são serializadas diretamente em JSON.</li>
 *   <li>{@code @RequestMapping("/condominio/reservas")} — Define a rota base para os recursos de reservas.</li>
 *   <li>{@code @RequiredArgsConstructor} — Cria pelo Lombok o construtor com os atributos marcados como {@code final}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/condominio/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * Endpoint para criar e confirmar um agendamento de área comum.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping} — Mapeia requisições HTTP POST para criação do recurso.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Exige que o solicitante esteja devidamente autenticado (com UUID válido de morador).</li>
     * </ul>
     * 
     * @param req DTO contendo a área de interesse, data e horários da reserva.
     * @param auth Objeto de autenticação injetado contendo o ID do usuário (X-User-Id) repassado pelo Gateway.
     * @return ResponseEntity contendo a reserva confirmada e o cabeçalho Location.
     */
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

    /**
     * Endpoint para cancelar uma reserva ativa.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @DeleteMapping("/{id}")} — Mapeia requisições HTTP DELETE para exclusão/cancelamento do recurso.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Exige autenticação prévia do solicitante.</li>
     * </ul>
     * 
     * @param id UUID único da reserva que se deseja cancelar.
     * @param auth Objeto de autenticação para validar a propriedade da reserva.
     * @return ResponseEntity indicando sucesso sem conteúdo.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id, Authentication auth) {
        UUID moradorId = UUID.fromString(auth.getName());
        reservaService.cancelar(id, moradorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para listar todas as reservas vigentes filtrando por área comum e data de interesse.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping} — Mapeia requisições HTTP GET sem caminhos adicionais.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Qualquer usuário autenticado pode pesquisar a disponibilidade.</li>
     * </ul>
     * 
     * @param areaComumId ID da área comum que se deseja consultar.
     * @param data Data de interesse formatada.
     * @return ResponseEntity com a lista de reservas ativas encontradas.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaResponse>> listar(
            @RequestParam UUID areaComumId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(reservaService.listar(areaComumId, data));
    }
}
