package com.condoplus.condominio.convivencia.service;

import com.condoplus.condominio.convivencia.domain.Comunicado;
import com.condoplus.condominio.convivencia.domain.PublicoAlvo;
import com.condoplus.condominio.convivencia.dto.ComunicadoResponse;
import com.condoplus.condominio.convivencia.dto.NovoComunicadoRequest;
import com.condoplus.condominio.convivencia.repository.ComunicadoRepository;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.exception.PessoaNaoEncontradaException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Serviço responsável pela publicação, remoção e listagem de Comunicados do condomínio.
 */
@Service
@Slf4j
public class ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;
    private final PessoaRepository pessoaRepository;
    private final Counter comunicadosCounter;

    public ComunicadoService(ComunicadoRepository comunicadoRepository,
                             PessoaRepository pessoaRepository,
                             MeterRegistry meterRegistry) {
        this.comunicadoRepository = comunicadoRepository;
        this.pessoaRepository = pessoaRepository;
        this.comunicadosCounter = Counter.builder("condoplus.comunicados.publicados")
                .description("Quantidade total de comunicados publicados no condominio")
                .register(meterRegistry);
    }

    /**
     * Publica um comunicado geral ou segmentado (exceto para bloco específico) na base de dados.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre uma transação comum de escrita (READ COMMITTED) para persistência.</li>
     * </ul>
     * 
     * @param req DTO contendo o título, conteúdo e o público-alvo do comunicado.
     * @param autorId ID único (UUID) do autor do comunicado (ex: síndico).
     * @return ComunicadoResponse contendo os dados do comunicado cadastrado.
     * @throws PessoaNaoEncontradaException se o autor especificado não for localizado.
     * @throws IllegalArgumentException se o público-alvo for BLOCO_ESPECIFICO sem passar pela rota especializada.
     */
    @Transactional
    public ComunicadoResponse publicar(NovoComunicadoRequest req, UUID autorId) {
        log.info("Publicando comunicado: titulo={}, autorId={}", req.titulo(), autorId);

        if (!pessoaRepository.existsById(autorId)) {
            throw new PessoaNaoEncontradaException(autorId);
        }

        Comunicado comunicado = new Comunicado();
        comunicado.setTitulo(req.titulo());
        comunicado.setMensagem(req.conteudo());
        comunicado.setPublicoAlvo(req.visibilidade());
        comunicado.setAutorId(AggregateReference.to(autorId));
        comunicado.setDataPublicacao(LocalDateTime.now());

        if (req.visibilidade() == PublicoAlvo.BLOCO_ESPECIFICO) {
            throw new IllegalArgumentException("Para a visibilidade BLOCO_ESPECIFICO, use o endpoint específico fornecendo o bloco destino.");
        }

        Comunicado salvo = comunicadoRepository.save(comunicado);
        this.comunicadosCounter.increment();
        log.info("Comunicado publicado com sucesso. id={}", salvo.getId());

        return ComunicadoResponse.fromEntity(salvo);
    }

    /**
     * Publica um comunicado destinado a um bloco residencial específico.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre uma transação de escrita (READ COMMITTED) para persistência segura.</li>
     * </ul>
     * 
     * @param req DTO contendo as informações de título e conteúdo do comunicado.
     * @param autorId ID único (UUID) do autor da publicação.
     * @param bloco String contendo o identificador do bloco alvo (ex: "A", "B").
     * @return ComunicadoResponse contendo o comunicado criado com escopo específico de bloco.
     * @throws PessoaNaoEncontradaException se o autor especificado não for localizado.
     * @throws IllegalArgumentException se o bloco informado for nulo ou vazio.
     */
    @Transactional
    public ComunicadoResponse publicarParaBloco(NovoComunicadoRequest req, UUID autorId, String bloco) {
        log.info("Publicando comunicado para bloco: titulo={}, bloco={}, autorId={}", req.titulo(), bloco, autorId);

        if (!pessoaRepository.existsById(autorId)) {
            throw new PessoaNaoEncontradaException(autorId);
        }

        if (bloco == null || bloco.isBlank()) {
            throw new IllegalArgumentException("O bloco destino deve ser fornecido quando visibilidade é BLOCO_ESPECIFICO.");
        }

        Comunicado comunicado = new Comunicado();
        comunicado.setTitulo(req.titulo());
        comunicado.setMensagem(req.conteudo());
        comunicado.setPublicoAlvo(PublicoAlvo.BLOCO_ESPECIFICO);
        comunicado.setBlocoAlvo(bloco.toUpperCase().trim());
        comunicado.setAutorId(AggregateReference.to(autorId));
        comunicado.setDataPublicacao(LocalDateTime.now());

        Comunicado salvo = comunicadoRepository.save(comunicado);
        this.comunicadosCounter.increment();
        log.info("Comunicado para o bloco {} publicado com sucesso. id={}", bloco, salvo.getId());

        return ComunicadoResponse.fromEntity(salvo);
    }

    /**
     * Busca um comunicado pelo seu ID único na base de dados.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Otimiza a consulta ao banco de dados com transação de leitura.</li>
     * </ul>
     * 
     * @param id ID único do comunicado.
     * @return ComunicadoResponse contendo os dados do comunicado encontrado.
     * @throws RuntimeException se o comunicado não for localizado.
     */
    @Transactional(readOnly = true)
    public ComunicadoResponse buscarPorId(UUID id) {
        return comunicadoRepository.findById(id)
                .map(ComunicadoResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Comunicado não encontrado com o ID: " + id));
    }

    /**
     * Lista todos os comunicados cadastrados de forma paginada e ordenada por data decrescente.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Otimiza o acesso a dados de leitura.</li>
     * </ul>
     * 
     * @param page Número da página a ser retornada (0-indexada).
     * @param size Quantidade de elementos por página.
     * @return Lista com os DTOs dos comunicados recentes encontrados.
     */
    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listarTodos(int page, int size) {
        int offset = page * size;
        return comunicadoRepository.findRecentes(size, offset).stream()
                .map(ComunicadoResponse::fromEntity)
                .toList();
    }

    /**
     * Filtra e lista comunicados com base na sua classificação de público-alvo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Otimiza o acesso a dados de leitura.</li>
     * </ul>
     * 
     * @param publicoAlvo Enum especificando a classificação (ex: MORADORES, PROPRIETARIOS).
     * @return Lista contendo os DTOs dos comunicados que coincidem com a visibilidade informada.
     */
    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listarPorPublicoAlvo(PublicoAlvo publicoAlvo) {
        return comunicadoRepository.findByPublicoAlvo(publicoAlvo).stream()
                .map(ComunicadoResponse::fromEntity)
                .toList();
    }

    /**
     * Exclui permanentemente um comunicado do sistema.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre uma transação comum de modificação de dados.</li>
     * </ul>
     * 
     * @param id ID único do comunicado que será removido.
     * @throws RuntimeException se o comunicado especificado não existir na base.
     */
    @Transactional
    public void remover(UUID id) {
        log.info("Removendo comunicado: {}", id);
        if (!comunicadoRepository.existsById(id)) {
            throw new RuntimeException("Comunicado não encontrado com o ID: " + id);
        }
        comunicadoRepository.deleteById(id);
    }
}
