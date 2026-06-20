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

/**
 * Controller REST responsável por expor endpoints para o gerenciamento e cadastro de veículos no condomínio.
 * 
 * <p>Este controlador atua no monitoramento de automóveis associados a vagas das unidades residenciais,
 * permitindo buscas por ID, listas globais e filtros direcionados por unidade habitacional.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @RestController} — Declara esta classe como um controlador Spring MVC REST gerenciando respostas JSON.</li>
 *   <li>{@code @RequestMapping("/condominio/veiculos")} — Define o prefixo de URL base para recursos de veículos.</li>
 *   <li>{@code @RequiredArgsConstructor} — Cria pelo Lombok o construtor contendo os campos finais obrigatórios.</li>
 * </ul>
 */
@RestController
@RequestMapping("/condominio/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    /**
     * Serviço responsável pelas operações e regras de negócio de veículos.
     */
    private final VeiculoService veiculoService;

    /**
     * Endpoint para cadastrar um novo veículo e vinculá-lo a uma vaga de unidade residencial.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping} — Mapeia requisições HTTP POST para cadastro de veículo.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'MORADOR')")} — Permite o cadastro aos moradores da unidade e aos perfis administrativos.</li>
     * </ul>
     * 
     * @param req DTO contendo a placa, modelo, cor e ID da unidade residencial.
     * @return ResponseEntity contendo os dados do veículo cadastrado e o cabeçalho HTTP Location com URI gerada.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'MORADOR')")
    public ResponseEntity<VeiculoResponse> cadastrar(@Valid @RequestBody NovoVeiculoRequest req) {
        VeiculoResponse criado = veiculoService.cadastrar(req);
        return ResponseEntity
                .created(URI.create("/condominio/veiculos/" + criado.id()))
                .body(criado);
    }

    /**
     * Endpoint para buscar um veículo específico através de seu identificador exclusivo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/{id}")} — Mapeia requisições HTTP GET com variável de caminho.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite acesso a qualquer morador/usuário autenticado.</li>
     * </ul>
     * 
     * @param id ID único do veículo.
     * @return ResponseEntity contendo as informações localizadas do veículo.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VeiculoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    /**
     * Endpoint para listar todos os veículos cadastrados no condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping} — Mapeia requisições HTTP GET sem caminhos adicionais.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'PORTEIRO')")} — Permite acesso a perfis administrativos e operacionais da portaria.</li>
     * </ul>
     * 
     * @return ResponseEntity contendo a lista com todos os veículos.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'PORTEIRO')")
    public ResponseEntity<List<VeiculoResponse>> listarTodos() {
        return ResponseEntity.ok(veiculoService.listarTodos());
    }

    /**
     * Endpoint para listar todos os veículos ativos vinculados a uma unidade residencial específica.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/unidade/{unidadeId}")} — Mapeia requisições HTTP GET com variável de caminho para a unidade.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite acesso a qualquer morador/usuário autenticado.</li>
     * </ul>
     * 
     * @param unidadeId ID da unidade residencial.
     * @return ResponseEntity contendo a lista de veículos cadastrados da unidade residencial.
     */
    @GetMapping("/unidade/{unidadeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VeiculoResponse>> listarPorUnidade(@PathVariable UUID  unidadeId) {
        return ResponseEntity.ok(veiculoService.listarPorUnidade(unidadeId));
    }

    /**
     * Endpoint para desativar logicamente (soft delete) um veículo cadastrado.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @DeleteMapping("/{id}")} — Mapeia requisições HTTP DELETE para inativação física/lógica do recurso.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'MORADOR')")} — Permite desativação ao morador responsável da unidade e perfis de gestão.</li>
     * </ul>
     * 
     * @param id ID do veículo a ser desativado.
     * @return ResponseEntity com status HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO', 'MORADOR')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        veiculoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
