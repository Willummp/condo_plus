package com.condoplus.condominio.convivencia.service;

import com.condoplus.condominio.convivencia.domain.AreaComum;
import com.condoplus.condominio.convivencia.dto.AreaComumResponse;
import com.condoplus.condominio.convivencia.dto.NovaAreaComumRequest;
import com.condoplus.condominio.convivencia.repository.AreaComumRepository;
import com.condoplus.condominio.exception.AreaComumNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Serviço responsável pelo gerenciamento cadastral e controle de ativação das Áreas Comuns.
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
public class AreaComumService {

    private final AreaComumRepository areaComumRepository;

    /**
     * Cadastra uma nova área comum no condomínio se o nome for exclusivo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre uma transação comum de escrita (READ COMMITTED) para persistência.</li>
     * </ul>
     * 
     * @param req DTO contendo o nome, capacidade, regras e valor de agendamento da área.
     * @return AreaComumResponse com os dados cadastrados.
     * @throws RuntimeException se já houver área comum cadastrada com o mesmo nome.
     */
    @Transactional
    public AreaComumResponse cadastrar(NovaAreaComumRequest req) {
        log.info("Cadastrando nova área comum com nome: {}", req.nome());

        if (areaComumRepository.existsByNome(req.nome())) {
            throw new RuntimeException("Já existe uma área comum cadastrada com o nome: " + req.nome());
        }

        AreaComum area = new AreaComum();
        area.setNome(req.nome());
        area.setCapacidadeMaxima(req.capacidadeMaxima());
        area.setValorReserva(req.valorReserva());
        area.setRegras(req.regras());
        area.setAtiva(true);

        AreaComum salva = areaComumRepository.save(area);
        log.info("Área comum cadastrada com sucesso. id={}", salva.getId());

        return AreaComumResponse.fromEntity(salva);
    }

    /**
     * Busca os dados de uma área comum utilizando seu identificador exclusivo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco.</li>
     * </ul>
     * 
     * @param id ID único da área comum.
     * @return AreaComumResponse com os dados correspondentes.
     * @throws AreaComumNaoEncontradaException se a área comum não for localizada.
     */
    @Transactional(readOnly = true)
    public AreaComumResponse buscarPorId(UUID id) {
        return areaComumRepository.findById(id)
                .map(AreaComumResponse::fromEntity)
                .orElseThrow(() -> new AreaComumNaoEncontradaException(id));
    }

    /**
     * Lista todas as áreas comuns cadastradas, permitindo filtro para apenas as ativas.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco.</li>
     * </ul>
     * 
     * @param apenasAtivas se {@code true}, retorna apenas as áreas com status ativo.
     * @return Lista contendo os DTOs das áreas comuns encontradas.
     */
    @Transactional(readOnly = true)
    public List<AreaComumResponse> listarTodas(boolean apenasAtivas) {
        if (apenasAtivas) {
            return areaComumRepository.findAllAtivas().stream()
                    .map(AreaComumResponse::fromEntity)
                    .toList();
        }
        return StreamSupport.stream(areaComumRepository.findAll().spliterator(), false)
                .map(AreaComumResponse::fromEntity)
                .toList();
    }

    /**
     * Atualiza os dados de uma área comum específica.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Garante atomicidade nas alterações dos dados da área comum.</li>
     * </ul>
     * 
     * @param id ID único da área que se deseja atualizar.
     * @param req DTO contendo as novas informações para atualização.
     * @return AreaComumResponse contendo a entidade atualizada.
     * @throws AreaComumNaoEncontradaException se a área comum não for localizada.
     */
    @Transactional
    public AreaComumResponse atualizar(UUID id, NovaAreaComumRequest req) {
        log.info("Atualizando área comum: {}", id);
        AreaComum area = areaComumRepository.findById(id)
                .orElseThrow(() -> new AreaComumNaoEncontradaException(id));

        area.setNome(req.nome());
        area.setCapacidadeMaxima(req.capacidadeMaxima());
        area.setValorReserva(req.valorReserva());
        area.setRegras(req.regras());

        AreaComum salva = areaComumRepository.save(area);
        log.info("Área comum atualizada com sucesso. id={}", id);

        return AreaComumResponse.fromEntity(salva);
    }

    /**
     * Desativa uma área comum, bloqueando temporariamente novos agendamentos nela.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre transação para alteração de status.</li>
     * </ul>
     * 
     * @param id ID da área comum que se deseja desativar.
     * @throws AreaComumNaoEncontradaException se a área comum não for localizada.
     */
    @Transactional
    public void desativar(UUID id) {
        log.info("Desativando área comum: {}", id);
        AreaComum area = areaComumRepository.findById(id)
                .orElseThrow(() -> new AreaComumNaoEncontradaException(id));
        
        area.setAtiva(false);
        areaComumRepository.save(area);
        log.info("Área comum desativada com sucesso. id={}", id);
    }

    /**
     * Reativa uma área comum desativada anteriormente.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre transação para alteração de status.</li>
     * </ul>
     * 
     * @param id ID da área comum que se deseja reativar.
     * @throws AreaComumNaoEncontradaException se a área comum não for localizada.
     */
    @Transactional
    public void ativar(UUID id) {
        log.info("Ativando área comum: {}", id);
        AreaComum area = areaComumRepository.findById(id)
                .orElseThrow(() -> new AreaComumNaoEncontradaException(id));
        
        area.setAtiva(true);
        areaComumRepository.save(area);
        log.info("Área comum ativada com sucesso. id={}", id);
    }
}
