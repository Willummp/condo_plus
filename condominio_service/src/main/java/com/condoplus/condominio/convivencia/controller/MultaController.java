package com.condoplus.condominio.convivencia.controller;

import com.condoplus.condominio.convivencia.domain.StatusMulta;
import com.condoplus.condominio.convivencia.dto.MultaResponse;
import com.condoplus.condominio.convivencia.dto.NovaMultaRequest;
import com.condoplus.condominio.convivencia.service.MultaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controller responsável por expor os endpoints REST de aplicação e atualização de status de Multas.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @RestController} — Identifica a classe como um controlador Spring REST, retornando dados JSON serializados diretamente nas respostas.</li>
 *   <li>{@code @RequestMapping("/condominio/multas")} — Define a rota base para os recursos de multas.</li>
 *   <li>{@code @RequiredArgsConstructor} — Cria pelo Lombok o construtor com os atributos marcados como {@code final}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/condominio/multas")
@RequiredArgsConstructor
public class MultaController {

    private final MultaService multaService;

    /**
     * Endpoint para aplicar uma nova multa a uma unidade condominial.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping} — Mapeia requisições HTTP POST para esta rota de aplicação.</li>
     *   <li>{@code @PreAuthorize("hasRole('SINDICO')")} — Limita a aplicação de multas exclusivamente ao usuário com a role de SINDICO.</li>
     * </ul>
     * 
     * @param req DTO contendo a unidade alvo, valor, motivo, categoria e anexo da multa.
     * @param auth Objeto contendo o UUID do síndico que está aplicando a multa.
     * @return ResponseEntity contendo a multa aplicada e o cabeçalho Location.
     */
    @PostMapping
    @PreAuthorize("hasRole('SINDICO')")
    public ResponseEntity<MultaResponse> aplicar(
            @Valid @RequestBody NovaMultaRequest req,
            Authentication auth) {
        UUID aplicadorId = UUID.fromString(auth.getName());
        MultaResponse criada = multaService.aplicar(req, aplicadorId);
        return ResponseEntity
                .created(URI.create("/condominio/multas/" + criada.id()))
                .body(criada);
    }

    /**
     * Endpoint para buscar uma multa específica pelo seu ID único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/{id}")} — Mapeia requisições HTTP GET contendo variável de caminho.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Exige autenticação básica do solicitante.</li>
     * </ul>
     * 
     * @param id UUID único da multa que se deseja buscar.
     * @return ResponseEntity contendo os dados da multa encontrada.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MultaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(multaService.buscarPorId(id));
    }

    /**
     * Endpoint para listar todas as multas de uma unidade, com a opção de filtrar pelo status da multa.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/unidade/{unidadeId}")} — Mapeia requisições HTTP GET filtrando pelo ID único da unidade.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Qualquer morador autenticado da unidade pode consultar o seu histórico de multas.</li>
     * </ul>
     * 
     * @param unidadeId ID único da unidade pesquisada.
     * @param status Status da multa para filtro opcional (ex: PENDENTE, PAGA).
     * @return ResponseEntity com a lista de multas filtradas encontradas.
     */
    @GetMapping("/unidade/{unidadeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MultaResponse>> listarPorUnidade(
            @PathVariable UUID unidadeId,
            @RequestParam(required = false) StatusMulta status) {
        return ResponseEntity.ok(multaService.listarPorUnidade(unidadeId, status));
    }

    /**
     * Endpoint para atualizar o status de uma multa existente (ex: liquidar pagamento ou cancelar multa).
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PatchMapping("/{id}/status")} — Mapeia requisições HTTP PATCH.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita a alteração a administradores e ao síndico.</li>
     * </ul>
     * 
     * @param id ID único da multa.
     * @param status Novo status a ser aplicado.
     * @return ResponseEntity contendo a multa após a atualização de status.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<MultaResponse> atualizarStatus(
            @PathVariable UUID id,
            @RequestParam StatusMulta status) {
        return ResponseEntity.ok(multaService.atualizarStatus(id, status));
    }
}
