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

/**
 * Controller responsável por expor os endpoints REST de gerenciamento de Pessoas.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @RestController} — Combina {@code @Controller} e {@code @ResponseBody}, indicando que as respostas de todos os métodos serão serializadas diretamente no corpo HTTP (geralmente JSON).</li>
 *   <li>{@code @RequestMapping("/condominio/pessoas")} — Define o prefixo de rota padrão para todos os endpoints deste controlador.</li>
 *   <li>{@code @RequiredArgsConstructor} — Gera pelo Lombok o construtor com argumentos para os atributos marcados como {@code final}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/condominio/pessoas")
@RequiredArgsConstructor
public class PessoaController {

    private final PessoaService pessoaService;

    /**
     * Endpoint para cadastrar uma nova pessoa fisicamente e solicitar credenciais no IAM.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping} — Mapeia requisições do tipo HTTP POST para esta rota.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Restringe o acesso a este recurso, exigindo que o usuário autenticado tenha o papel de ADMIN ou SINDICO no token fornecido pelo Gateway.</li>
     * </ul>
     * 
     * @param req DTO contendo os dados cadastrais da nova pessoa.
     * @return ResponseEntity contendo os dados da pessoa criada e o cabeçalho Location do recurso.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<PessoaResponse> cadastrar(@Valid @RequestBody NovaPessoaRequest req) {
        PessoaResponse criada = pessoaService.cadastrar(req);
        return ResponseEntity
                .created(URI.create("/condominio/pessoas/" + criada.id()))
                .body(criada);
    }

    /**
     * Endpoint para buscar uma pessoa específica utilizando seu ID único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/{id}")} — Mapeia requisições HTTP GET contendo uma variável de caminho (path variable).</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Exige apenas que o usuário esteja autenticado, independentemente de seu papel específico.</li>
     * </ul>
     * 
     * @param id UUID único da pessoa buscada.
     * @return ResponseEntity contendo a resposta com os dados da pessoa.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PessoaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pessoaService.buscarPorId(id));
    }

    /**
     * Endpoint para buscar uma pessoa filtrando pelo CPF.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping(params = "cpf")} — Mapeia requisições HTTP GET que possuem o parâmetro de query {@code cpf} especificado.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'PORTEIRO')")} — Permite acesso apenas a cargos administrativos e operacionais da portaria.</li>
     * </ul>
     * 
     * @param cpf CPF da pessoa pesquisada.
     * @return ResponseEntity contendo a pessoa correspondente.
     */
    @GetMapping(params = "cpf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'PORTEIRO')")
    public ResponseEntity<PessoaResponse> buscarPorCpf(@RequestParam String cpf) {
        return ResponseEntity.ok(pessoaService.buscarPorCpf(cpf));
    }

    /**
     * Endpoint para listar todas as pessoas cadastradas na base.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping} — Mapeia requisições HTTP GET sem caminhos adicionais.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Restringe o acesso a este recurso para ADMIN ou SINDICO.</li>
     * </ul>
     * 
     * @return ResponseEntity contendo a lista com todas as pessoas cadastradas.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<List<PessoaResponse>> listarTodas() {
        return ResponseEntity.ok(pessoaService.listarTodas());
    }
}
