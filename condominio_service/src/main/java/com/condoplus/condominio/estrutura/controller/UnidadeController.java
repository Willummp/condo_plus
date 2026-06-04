package com.condoplus.condominio.estrutura.controller;

import com.condoplus.condominio.estrutura.domain.*;
import com.condoplus.condominio.estrutura.dto.*;
import com.condoplus.condominio.estrutura.repository.UnidadeRepository;
import com.condoplus.condominio.estrutura.service.EscopoDerivacaoService;
import com.condoplus.condominio.exception.UnidadeNaoEncontradaException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Controller REST responsável por expor os endpoints de gerenciamento de Unidades e suas Vinculações.
 * 
 * <p>Este controlador atua como um dos principais pontos de entrada do bounded context de Estrutura,
 * permitindo o cadastro de imóveis, associações de moradores/inquilinos (Vinculações) e o encerramento das mesmas.
 * 
 * <p>Padrão de Rota e API Gateway:
 * Os endpoints iniciam com {@code /condominio/unidades}. O prefixo {@code /api} é filtrado dinamicamente
 * pelo Spring Cloud Gateway (api-gateway) por meio do filtro {@code StripPrefix=1}.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @RestController} — Identifica a classe como um controlador REST gerenciado pelo Spring IoC.</li>
 *   <li>{@code @RequestMapping("/condominio/unidades")} — Define a rota raiz para os recursos de unidades.</li>
 *   <li>{@code @RequiredArgsConstructor} — Cria pelo Lombok o construtor contendo os campos finais obrigatórios.</li>
 * </ul>
 */
@RestController
@RequestMapping("/condominio/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    /**
     * Repositório de dados para busca e persistência de unidades.
     */
    private final UnidadeRepository unidadeRepository;

    /**
     * Serviço responsável pelo cálculo e derivação dinâmica de escopos (SOCIAL, LEGAL, FINANCEIRO).
     */
    private final EscopoDerivacaoService escopoDerivacaoService;

    /**
     * Endpoint para criar e cadastrar uma nova unidade residencial (apartamento ou casa) no condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping} — Mapeia requisições HTTP POST para criação de unidades.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita o cadastro apenas a perfis de gestão administrativa.</li>
     * </ul>
     * 
     * @param req DTO contendo o número, bloco opcional e o tipo de unidade.
     * @return ResponseEntity contendo a unidade criada e o cabeçalho HTTP Location com o ID gerado.
     * @throws RuntimeException se já constar no banco de dados uma unidade com a mesma combinação de número e bloco.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<UnidadeResponse> criar(@Valid @RequestBody NovaUnidadeRequest req) {
        if (unidadeRepository.existsByNumeroAndBloco(req.numero(), req.bloco())) {
            throw new RuntimeException("Já existe unidade " + req.numero() +
                (req.bloco() != null ? " no bloco " + req.bloco() : ""));
        }
        Unidade unidade = Unidade.criar(req.numero(), req.bloco(), req.tipo());
        Unidade salva = unidadeRepository.save(unidade);
        return ResponseEntity
            .created(URI.create("/condominio/unidades/" + salva.getId()))
            .body(UnidadeResponse.fromEntity(salva));
    }

    /**
     * Endpoint para buscar uma unidade específica pelo seu ID único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/{id}")} — Mapeia requisições HTTP GET com variável de caminho.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite visualização a qualquer usuário logado na plataforma.</li>
     * </ul>
     * 
     * @param id ID único da unidade habitacional procurada.
     * @return ResponseEntity contendo os dados da unidade.
     * @throws UnidadeNaoEncontradaException se a unidade correspondente ao ID não for localizada.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnidadeResponse> buscar(@PathVariable UUID id) {
        return unidadeRepository.findById(id)
            .map(u -> ResponseEntity.ok(UnidadeResponse.fromEntity(u)))
            .orElseThrow(() -> new UnidadeNaoEncontradaException(id));
    }

    /**
     * Endpoint para listar todas as unidades registradas, permitindo filtro opcional por bloco residencial.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping} — Mapeia requisições HTTP GET sem caminhos adicionais.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite acesso a qualquer morador/usuário autenticado.</li>
     * </ul>
     * 
     * @param bloco Parâmetro opcional contendo o nome/bloco de interesse.
     * @return ResponseEntity contendo a lista com as unidades correspondentes ao filtro.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UnidadeResponse>> listar(
            @RequestParam(required = false) String bloco) {
        List<UnidadeResponse> result = bloco != null
            ? unidadeRepository.findAllByBloco(bloco).stream().map(UnidadeResponse::fromEntity).toList()
            : StreamSupport.stream(unidadeRepository.findAll().spliterator(), false)
                           .map(UnidadeResponse::fromEntity).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint para vincular (associar) uma pessoa física a uma unidade residencial.
     * 
     * <p>Este método também desencadeia o recalculo automático de todos os escopos das vinculações
     * contidas na unidade e salva a entidade no banco de dados.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PostMapping("/{unidadeId}/vinculacoes")} — Mapeia requisições HTTP POST para criação de vínculos.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Permite a associação apenas a perfis administrativos.</li>
     * </ul>
     * 
     * @param unidadeId ID da unidade residencial.
     * @param req DTO contendo o ID da pessoa, o tipo do vínculo e a data de início da vigência.
     * @return ResponseEntity contendo a vinculação criada com status HTTP 201 Created.
     * @throws UnidadeNaoEncontradaException se a unidade residencial correspondente ao ID não for localizada.
     */
    @PostMapping("/{unidadeId}/vinculacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<VinculacaoResponse> vincular(
            @PathVariable UUID unidadeId,
            @Valid @RequestBody NovaVinculacaoRequest req) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
            .orElseThrow(() -> new UnidadeNaoEncontradaException(unidadeId));

        Vinculacao v = Vinculacao.criar(req.pessoaId(), req.tipo(), req.dataInicio());
        unidade.adicionarVinculacao(v);
        escopoDerivacaoService.derivarEscoposDaUnidade(unidade);
        unidadeRepository.save(unidade);

        Vinculacao criada = unidade.getVinculacoes().stream()
            .filter(vc -> vc.getPessoaId().getId().equals(req.pessoaId()))
            .findFirst()
            .orElseThrow();

        return ResponseEntity.status(201).body(VinculacaoResponse.fromEntity(criada));
    }

    /**
     * Endpoint para encerrar logicamente (inativar) uma vinculação de moradia ativa.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @PatchMapping("/vinculacoes/{vinculacaoId}/encerrar")} Mapeia requisições parciais HTTP PATCH para o encerramento do vínculo.</li>
     *   <li>{@code @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")} — Limita o encerramento do vínculo às funções de gestão administrativa.</li>
     * </ul>
     * 
     * @param vinculacaoId ID único da vinculação a ser encerrada.
     * @return ResponseEntity indicando sucesso sem conteúdo (HTTP 204 No Content).
     * @throws RuntimeException se a vinculação correspondente ao ID informado não for localizada em nenhuma unidade.
     */
    @PatchMapping("/vinculacoes/{vinculacaoId}/encerrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
    public ResponseEntity<Void> encerrarVinculacao(@PathVariable UUID vinculacaoId) {
        Unidade unidade = StreamSupport
            .stream(unidadeRepository.findAll().spliterator(), false)
            .filter(u -> u.getVinculacoes().stream().anyMatch(v -> vinculacaoId.equals(v.getId())))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Vinculação não encontrada: " + vinculacaoId));

        unidade.getVinculacoes().stream()
            .filter(v -> vinculacaoId.equals(v.getId()))
            .findFirst()
            .ifPresent(v -> v.encerrar(java.time.LocalDate.now()));

        escopoDerivacaoService.derivarEscoposDaUnidade(unidade);
        unidadeRepository.save(unidade);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para listar todas as vinculações de moradores/inquilinos atreladas a uma unidade residencial.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @GetMapping("/{id}/vinculacoes")} — Mapeia requisições HTTP GET com caminhos aninhados.</li>
     *   <li>{@code @PreAuthorize("isAuthenticated()")} — Permite acesso a qualquer morador/usuário autenticado.</li>
     * </ul>
     * 
     * @param id ID único da unidade habitacional procurada.
     * @param apenasAtivas Se {@code true}, retorna apenas os moradores ativos; se {@code false}, retorna também o histórico de moradores passados.
     * @return ResponseEntity contendo a lista com os moradores localizados de acordo com o filtro.
     * @throws UnidadeNaoEncontradaException se a unidade correspondente ao ID não for localizada.
     */
    @GetMapping("/{id}/vinculacoes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VinculacaoResponse>> listarVinculacoes(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivas) {
        Unidade unidade = unidadeRepository.findById(id)
            .orElseThrow(() -> new UnidadeNaoEncontradaException(id));

        List<VinculacaoResponse> vinculacoes = unidade.getVinculacoes().stream()
            .filter(v -> !apenasAtivas || v.isAtiva())
            .map(VinculacaoResponse::fromEntity)
            .toList();

        return ResponseEntity.ok(vinculacoes);
    }
}
