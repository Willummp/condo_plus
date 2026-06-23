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

/**
 * Controller responsável por expor os endpoints REST para publicação, remoção e busca de Comunicados.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @RestController} — Identifica a classe como um controlador Spring REST, retornando dados serializados diretamente em JSON.</li>
 *   <li>{@code @RequestMapping("/condominio/comunicados")} — Define a rota raiz para os recursos de comunicados.</li>
 *   <li>{@code @RequiredArgsConstructor} — Cria pelo Lombok o construtor com os atributos marcados como {@code final}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/condominio/comunicados")
@RequiredArgsConstructor
public class ComunicadoController {

    private final ComunicadoService comunicadoService;

    /**
     * Endpoint para publicar um comunicado de interesse geral do condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping} — Mapeia requisições HTTP POST para criação de um comunicado comum.</li>
     *   <li>{@code @PreAuthorize("hasRole('SINDICO')")} — Limita o envio aos usuários que tenham a role de SINDICO.</li>
     * </ul>
     * 
     * @param req DTO contendo o título, mensagem e visibilidade geral do comunicado.
     * @param auth Objeto de autenticação contendo o UUID do autor síndico.
     * @return ResponseEntity contendo o comunicado criado e o cabeçalho Location.
     */
    @PostMapping
    @PreAuthorize("hasRole('SINDICO')")
    public ResponseEntity<ComunicadoResponse> publicar(
            @Valid @RequestBody NovoComunicadoRequest req,
            Authentication auth) {
        UUID autorId = UUID.fromString(auth.getName());
        ComunicadoResponse criado = comunicadoService.publicar(req, autorId);
        return ResponseEntity
                .created(URI.create("/condominio/comunicados/" + criado.id()))
                .body(criado);
    }

    /**
     * Endpoint para publicar um comunicado direcionado especificamente para um bloco residencial.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping("/bloco/{bloco}")} — Mapeia requisições HTTP POST contendo a variável de caminho correspondente ao bloco residencial.</li>
     *   <li>{@code @PreAuthorize("hasRole('SINDICO')")} — Limita a publicação de avisos de bloco ao síndico do condomínio.</li>
     * </ul>
     * 
     * @param req DTO contendo o título e a mensagem do aviso.
     * @param bloco String contendo o identificador do bloco alvo (ex: "A", "C").
     * @param auth Objeto contendo as credenciais de autenticação do autor.
     * @return ResponseEntity com o comunicado segmentado criado e o cabeçalho Location.
     */
    @PostMapping("/bloco/{bloco}")
    @PreAuthorize("hasRole('SINDICO')")
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

    /**
     * Endpoint para buscar um comunicado específico pelo seu ID único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/{id}")} — Mapeia requisições HTTP GET com variável de caminho.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite acesso a qualquer morador/usuário autenticado.</li>
     * </ul>
     * 
     * @param id UUID único do comunicado buscado.
     * @return ResponseEntity contendo o comunicado localizado.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComunicadoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(comunicadoService.buscarPorId(id));
    }

    /**
     * Endpoint para listar todos os comunicados cadastrados de forma paginada.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping} — Mapeia requisições HTTP GET sem caminhos adicionais.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Qualquer morador logado pode ver o mural de comunicados.</li>
     * </ul>
     * 
     * @param page Número da página a ser listada (valor padrão: 0).
     * @param size Quantidade de elementos por página (valor padrão: 10).
     * @return ResponseEntity contendo a lista com os comunicados da página correspondente.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComunicadoResponse>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(comunicadoService.listarTodos(page, size));
    }

    /**
     * Endpoint para excluir/remover permanentemente um comunicado.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @DeleteMapping("/{id}")} — Mapeia requisições HTTP DELETE para remoção do recurso.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita a exclusão aos papéis administrativos (ADMIN ou SINDICO).</li>
     * </ul>
     * 
     * @param id UUID único do comunicado que será apagado.
     * @return ResponseEntity indicando sucesso sem conteúdo.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        comunicadoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
