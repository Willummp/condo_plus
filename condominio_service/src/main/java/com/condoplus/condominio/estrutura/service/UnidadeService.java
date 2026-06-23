package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.domain.Unidade;
import com.condoplus.condominio.estrutura.domain.Vinculacao;
import com.condoplus.condominio.estrutura.dto.NovaUnidadeRequest;
import com.condoplus.condominio.estrutura.dto.NovaVinculacaoRequest;
import com.condoplus.condominio.estrutura.dto.UnidadeResponse;
import com.condoplus.condominio.estrutura.dto.VinculacaoResponse;
import com.condoplus.condominio.estrutura.repository.UnidadeRepository;
import com.condoplus.condominio.exception.UnidadeNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Serviço responsável por encapsular a lógica de negócio de Unidades e suas Vinculações.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final EscopoDerivacaoService escopoDerivacaoService;

    @Transactional
    public UnidadeResponse criar(NovaUnidadeRequest req) {
        log.info("Criando nova unidade: numero={}, bloco={}", req.numero(), req.bloco());

        if (unidadeRepository.existsByNumeroAndBloco(req.numero(), req.bloco())) {
            throw new RuntimeException("Já existe unidade " + req.numero() +
                    (req.bloco() != null ? " no bloco " + req.bloco() : ""));
        }

        Unidade unidade = Unidade.criar(req.numero(), req.bloco(), req.tipo());
        Unidade salva = unidadeRepository.save(unidade);
        
        log.info("Unidade criada com sucesso: id={}", salva.getId());
        return UnidadeResponse.fromEntity(salva);
    }

    @Transactional(readOnly = true)
    public UnidadeResponse buscar(UUID id) {
        return unidadeRepository.findById(id)
                .map(UnidadeResponse::fromEntity)
                .orElseThrow(() -> new UnidadeNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<UnidadeResponse> listar(String bloco) {
        if (bloco != null) {
            return unidadeRepository.findAllByBloco(bloco).stream()
                    .map(UnidadeResponse::fromEntity)
                    .toList();
        }
        return unidadeRepository.findAllOrdered().stream()
                .map(UnidadeResponse::fromEntity)
                .toList();
    }

    @Transactional
    public VinculacaoResponse vincular(UUID unidadeId, NovaVinculacaoRequest req) {
        log.info("Vinculando pessoa={} a unidade={}", req.pessoaId(), unidadeId);

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

        log.info("Vinculação criada com sucesso: vinculacaoId={}", criada.getId());
        return VinculacaoResponse.fromEntity(criada);
    }

    @Transactional
    public void encerrarVinculacao(UUID vinculacaoId) {
        log.info("Encerrando vinculacaoId={}", vinculacaoId);

        Unidade unidade = unidadeRepository.findByVinculacaoId(vinculacaoId)
                .orElseThrow(() -> new RuntimeException("Vinculação não encontrada na base: " + vinculacaoId));

        unidade.getVinculacoes().stream()
                .filter(v -> vinculacaoId.equals(v.getId()))
                .findFirst()
                .ifPresent(v -> v.encerrar(LocalDate.now()));

        escopoDerivacaoService.derivarEscoposDaUnidade(unidade);
        unidadeRepository.save(unidade);
        
        log.info("Vinculação {} encerrada com sucesso na unidade {}", vinculacaoId, unidade.getId());
    }

    @Transactional(readOnly = true)
    public List<VinculacaoResponse> listarVinculacoes(UUID unidadeId, boolean apenasAtivas) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new UnidadeNaoEncontradaException(unidadeId));

        return unidade.getVinculacoes().stream()
                .filter(v -> !apenasAtivas || v.isAtiva())
                .map(VinculacaoResponse::fromEntity)
                .toList();
    }
}
