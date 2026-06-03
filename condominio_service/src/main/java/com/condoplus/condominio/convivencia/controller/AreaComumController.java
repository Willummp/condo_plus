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

/**
 * Controller responsável por expor os endpoints REST de cadastro e ativação de Áreas Comuns.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @RestController} — Identifica a classe como um controlador Spring REST, retornando objetos serializados em JSON.</li>
 *   <li>{@code @RequestMapping("/condominio/areas-comuns")} — Define a rota raiz para os endpoints deste controlador.</li>
 *   <li>{@code @RequiredArgsConstructor} — Cria pelo Lombok o construtor com argumentos para os campos {@code final}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/condominio/areas-comuns")
@RequiredArgsConstructor
public class AreaComumController {

    private final AreaComumService areaComumService;

    /**
     * Endpoint para cadastrar uma nova área comum (ex: piscina, academia).
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping} — Mapeia requisições HTTP POST para esta rota.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Restringe o acesso aos administradores e ao síndico.</li>
     * </ul>
     * 
     * @param req DTO contendo os dados básicos da nova área comum.
     * @return ResponseEntity contendo a área cadastrada e o cabeçalho Location correspondente.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<AreaComumResponse> cadastrar(@Valid @RequestBody NovaAreaComumRequest req) {
        AreaComumResponse criada = areaComumService.cadastrar(req);
        return ResponseEntity
                .created(URI.create("/condominio/areas-comuns/" + criada.id()))
                .body(criada);
    }

    /**
     * Endpoint para buscar uma área comum específica pelo seu ID único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/{id}")} — Mapeia requisições HTTP GET com variável de caminho.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite acesso a qualquer usuário devidamente autenticado no sistema.</li>
     * </ul>
     * 
     * @param id UUID único da área comum pesquisada.
     * @return ResponseEntity com os dados da área encontrada.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AreaComumResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(areaComumService.buscarPorId(id));
    }

    /**
     * Endpoint para listar todas as áreas comuns do condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping} — Mapeia requisições HTTP GET sem caminhos adicionais.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite acesso a qualquer usuário autenticado.</li>
     * </ul>
     * 
     * @param apenasAtivas se {@code true}, retorna somente áreas comuns prontas para reservas.
     * @return ResponseEntity contendo a lista das áreas comuns correspondentes.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AreaComumResponse>> listarTodas(
            @RequestParam(required = false, defaultValue = "true") boolean apenasAtivas) {
        return ResponseEntity.ok(areaComumService.listarTodas(apenasAtivas));
    }

    /**
     * Endpoint para atualizar as regras e configurações de uma área comum existente.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PutMapping("/{id}")} — Mapeia requisições HTTP PUT para atualização total do recurso.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita a alteração a administradores e ao síndico.</li>
     * </ul>
     * 
     * @param id UUID único da área comum que se deseja atualizar.
     * @param req DTO contendo os novos dados atualizados.
     * @return ResponseEntity com a área comum após a modificação ser salva.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<AreaComumResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody NovaAreaComumRequest req) {
        return ResponseEntity.ok(areaComumService.atualizar(id, req));
    }

    /**
     * Endpoint para desativar temporariamente uma área comum, bloqueando novas reservas.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PatchMapping("/{id}/desativar")} — Mapeia requisições HTTP PATCH para modificação de status do recurso.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita a alteração a cargos de gerência (ADMIN, SINDICO).</li>
     * </ul>
     * 
     * @param id ID único da área comum.
     * @return ResponseEntity indicando sucesso sem conteúdo adicional.
     */
    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        areaComumService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para reativar uma área comum desativada anteriormente.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PatchMapping("/{id}/ativar")} — Mapeia requisições HTTP PATCH.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita a alteração a cargos de gerência (ADMIN, SINDICO).</li>
     * </ul>
     * 
     * @param id ID único da área comum.
     * @return ResponseEntity indicando sucesso sem conteúdo.
     */
    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> ativar(@PathVariable UUID id) {
        areaComumService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
