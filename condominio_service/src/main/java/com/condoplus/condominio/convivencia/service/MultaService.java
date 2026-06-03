package com.condoplus.condominio.convivencia.service;

import com.condoplus.condominio.convivencia.domain.Multa;
import com.condoplus.condominio.convivencia.domain.StatusMulta;
import com.condoplus.condominio.convivencia.dto.MultaResponse;
import com.condoplus.condominio.convivencia.dto.NovaMultaRequest;
import com.condoplus.condominio.convivencia.repository.MultaRepository;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.estrutura.repository.UnidadeRepository;
import com.condoplus.condominio.exception.PessoaNaoEncontradaException;
import com.condoplus.condominio.exception.UnidadeNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Serviço responsável por gerenciar a aplicação, atualização de status e listagem de Multas do condomínio.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @Service} — Declara esta classe como um componente de serviço gerenciado pelo Spring IoC.</li>
 *   <li>{@code @RequiredArgsConstructor} — Gera pelo Lombok o construtor com argumentos para os campos {@code final}.</li>
 *   <li>{@code @Slf4j} — Injeta automaticamente o Logger SLF4J sob o atributo {@code log}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultaService {

    private final MultaRepository multaRepository;
    private final UnidadeRepository unidadeRepository;
    private final PessoaRepository pessoaRepository;

    /**
     * Aplica uma nova multa a uma unidade condominial.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre uma transação de escrita (READ COMMITTED) para persistência segura da multa.</li>
     * </ul>
     * 
     * @param req DTO contendo o ID da unidade, valor, motivo, categoria e anexo da evidência da multa.
     * @param aplicadorId ID único (UUID) do autor da aplicação (geralmente o síndico).
     * @return MultaResponse contendo a multa criada.
     * @throws UnidadeNaoEncontradaException se a unidade de destino não existir.
     * @throws PessoaNaoEncontradaException se o aplicador da multa não for localizado.
     */
    @Transactional
    public MultaResponse aplicar(NovaMultaRequest req, UUID aplicadorId) {
        log.info("Aplicando multa: unidadeId={}, valor={}, aplicadorId={}", req.unidadeId(), req.valor(), aplicadorId);

        if (!unidadeRepository.existsById(req.unidadeId())) {
            throw new UnidadeNaoEncontradaException(req.unidadeId());
        }

        if (!pessoaRepository.existsById(aplicadorId)) {
            throw new PessoaNaoEncontradaException(aplicadorId);
        }

        Multa multa = new Multa();
        multa.setUnidadeId(AggregateReference.to(req.unidadeId()));
        multa.setValor(req.valor());
        multa.setMotivo(req.motivo());
        multa.setCategoria(req.categoria());
        multa.setAnexoEvidenciaUrl(req.anexoEvidenciaUrl());
        multa.setDataAplicacao(LocalDateTime.now());
        multa.setDataVencimento(req.dataVencimento());
        multa.setStatus(StatusMulta.PENDENTE);
        multa.setAplicadaPorId(AggregateReference.to(aplicadorId));

        Multa salva = multaRepository.save(multa);
        log.info("Multa aplicada com sucesso. id={}", salva.getId());

        return MultaResponse.fromEntity(salva);
    }

    /**
     * Busca os dados de uma multa específica utilizando seu ID único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco.</li>
     * </ul>
     * 
     * @param id ID único da multa.
     * @return MultaResponse contendo os dados correspondentes.
     * @throws RuntimeException se a multa especificada não for localizada.
     */
    @Transactional(readOnly = true)
    public MultaResponse buscarPorId(UUID id) {
        return multaRepository.findById(id)
                .map(MultaResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Multa não encontrada com o ID: " + id));
    }

    /**
     * Lista todas as multas cadastradas no condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco.</li>
     * </ul>
     * 
     * @return Lista contendo os DTOs de todas as multas encontradas.
     */
    @Transactional(readOnly = true)
    public List<MultaResponse> listarTodas() {
        return StreamSupport.stream(multaRepository.findAll().spliterator(), false)
                .map(MultaResponse::fromEntity)
                .toList();
    }

    /**
     * Filtra e lista multas associadas a uma unidade específica, permitindo filtro adicional por status.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco.</li>
     * </ul>
     * 
     * @param unidadeId ID da unidade alvo da consulta.
     * @param status Status da multa para filtro (ex: PENDENTE, PAGA). Se nulo, retorna todas da unidade.
     * @return Lista com os DTOs das multas filtradas.
     * @throws UnidadeNaoEncontradaException se a unidade de destino não for localizada.
     */
    @Transactional(readOnly = true)
    public List<MultaResponse> listarPorUnidade(UUID unidadeId, StatusMulta status) {
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new UnidadeNaoEncontradaException(unidadeId);
        }
        if (status != null) {
            return multaRepository.findByUnidadeEStatus(unidadeId, status).stream()
                    .map(MultaResponse::fromEntity)
                    .toList();
        }
        return multaRepository.findByUnidade(unidadeId).stream()
                .map(MultaResponse::fromEntity)
                .toList();
    }

    /**
     * Atualiza o status de uma multa específica (ex: marcar como PAGA ou CANCELADA).
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre uma transação comum de escrita.</li>
     * </ul>
     * 
     * @param id ID único da multa que sofrerá alteração.
     * @param status Novo status a ser atribuído à multa.
     * @return MultaResponse contendo a multa atualizada.
     * @throws RuntimeException se a multa especificada não for localizada.
     */
    @Transactional
    public MultaResponse atualizarStatus(UUID id, StatusMulta status) {
        log.info("Atualizando status da multa: id={}, novoStatus={}", id, status);
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Multa não encontrada com o ID: " + id));
        
        multa.setStatus(status);
        Multa salva = multaRepository.save(multa);
        log.info("Status da multa atualizado com sucesso. id={}", id);
        
        return MultaResponse.fromEntity(salva);
    }
}
