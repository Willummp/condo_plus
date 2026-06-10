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

/**
 * Controller REST responsável por expor endpoints para o gerenciamento de funcionários do condomínio.
 * 
 * <p>Este controlador gerencia requisições de admissão, desligamento e consultas cadastrais,
 * integrando-se com as regras de autorização do Spring Security enviadas pelo API Gateway.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @RestController} — Identifica esta classe como um controlador Spring REST que serializa as respostas em JSON.</li>
 *   <li>{@code @RequestMapping("/condominio/funcionarios")} — Define o caminho base de URL para o recurso de funcionários.</li>
 *   <li>{@code @RequiredArgsConstructor} — Cria pelo Lombok o construtor com os atributos finais declarados.</li>
 * </ul>
 */
@RestController
@RequestMapping("/condominio/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    /**
     * Serviço responsável pela orquestração das regras de negócio de funcionários.
     */
    private final FuncionarioService funcionarioService;

    /**
     * Endpoint para admitir um novo funcionário no condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping} — Mapeia requisições HTTP POST para admissão de funcionários.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita o acesso exclusivamente aos papéis administrativos (ADMIN ou SINDICO).</li>
     * </ul>
     * 
     * @param req DTO contendo o ID da pessoa, o cargo pretendido e a data de admissão.
     * @return ResponseEntity contendo os dados do funcionário criado e o cabeçalho HTTP Location correspondente.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<FuncionarioResponse> admitir(@Valid @RequestBody NovoFuncionarioRequest req) {
        FuncionarioResponse criado = funcionarioService.admitir(req);
        return ResponseEntity
                .created(URI.create("/condominio/funcionarios/" + criado.id()))
                .body(criado);
    }

    /**
     * Endpoint para buscar um funcionário específico através de seu ID exclusivo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/{id}")} — Mapeia requisições HTTP GET com variável de caminho.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite acesso a qualquer morador/usuário autenticado no sistema.</li>
     * </ul>
     * 
     * @param id ID do funcionário buscado.
     * @return ResponseEntity com as informações do funcionário localizado.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FuncionarioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    /**
     * Endpoint para listar todos os funcionários cadastrados, filtrando por cargo ou por status ativo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping} — Mapeia requisições HTTP GET sem caminhos adicionais.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'PORTEIRO')")} — Permite acesso aos perfis operacionais e de gestão.</li>
     * </ul>
     * 
     * @param apenasAtivos Flag para buscar apenas funcionários ativos (padrão: {@code true}).
     * @param cargo Parâmetro opcional para filtrar os funcionários ativos por um determinado {@link CargoFuncionario}.
     * @return ResponseEntity contendo a lista com os funcionários localizados.
     */
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

    /**
     * Endpoint para demitir logicamente (desligar) um funcionário do condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PatchMapping("/{id}/desligar")} Mapeia requisições parciais HTTP PATCH para desligamento lógico.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita a demissão às funções administrativas (ADMIN ou SINDICO).</li>
     * </ul>
     * 
     * @param id ID do funcionário a ser desligado.
     * @param dataDesligamento Data oficial de desligamento opcional (se nula, assume o dia atual).
     * @return ResponseEntity com status HTTP 204 No Content.
     */
    @PatchMapping("/{id}/desligar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> desligar(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataDesligamento) {
        
        funcionarioService.desligar(id, dataDesligamento);
        return ResponseEntity.noContent().build();
    }
}
